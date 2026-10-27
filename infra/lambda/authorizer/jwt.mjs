// =============================================================================
// Validação de JWT emitido pelo Keycloak — sem dependências externas.
//
// Tudo que é preciso está no módulo `node:crypto`: ele importa JWK direto
// (`format: 'jwk'`) e verifica assinatura RSA. Uma biblioteca como `jose`
// faria o mesmo, ao custo de um node_modules no pacote de deploy, de uma
// atualização de segurança a acompanhar e de ~300 KB no cold start.
//
// Este módulo é PURO de propósito: nada aqui conhece a AWS nem lê variáveis de
// ambiente. É o que permite testá-lo com `node --test`, sem simular nada da
// Lambda — ver jwt.test.mjs.
// =============================================================================

import { createPublicKey, createVerify } from 'node:crypto';

/**
 * Algoritmos aceitos, e apenas eles.
 *
 * A lista ser fechada é a defesa contra confusão de algoritmo, a falha clássica
 * de validação de JWT: um token com `alg: "none"` sem assinatura, ou com
 * `alg: "HS256"` assinado com a CHAVE PÚBLICA do JWKS como se fosse segredo
 * compartilhado. Em ambos os casos o token é forjável por quem conhece a chave
 * pública — que é pública. Validar `alg` contra uma lista de permissão, antes
 * de olhar qualquer outra coisa, elimina as duas.
 */
const ALGORITMOS_SUPORTADOS = Object.freeze({
  RS256: 'RSA-SHA256',
  RS384: 'RSA-SHA384',
  RS512: 'RSA-SHA512',
});

/**
 * Falha atribuível ao TOKEN, não à infraestrutura. Vira 403 no API Gateway.
 *
 * O `codigo` existe para uma decisão específica: só o caso `kid_desconhecido`
 * justifica rebuscar o JWKS (pode ser rotação de chave). Distinguir por código
 * em vez de inspecionar a mensagem evita que reescrever um texto de erro mude o
 * comportamento de rede da função.
 */
export class TokenInvalido extends Error {
  constructor(motivo, codigo = 'invalido') {
    super(motivo);
    this.name = 'TokenInvalido';
    this.codigo = codigo;
  }
}

/**
 * Quebra o token em header, payload e assinatura.
 *
 * Decodificar NÃO é validar: nada do que sai daqui é confiável antes de
 * `verificarAssinatura`. O header é lido primeiro apenas para descobrir qual
 * chave e qual algoritmo usar — e é exatamente por isso que o `alg` que vem
 * dele precisa passar por lista de permissão.
 */
export function decodificar(token) {
  if (typeof token !== 'string' || token.length === 0) {
    throw new TokenInvalido('token ausente');
  }

  const partes = token.split('.');
  if (partes.length !== 3) {
    throw new TokenInvalido('token nao tem tres segmentos');
  }

  const [headerB64, payloadB64, assinaturaB64] = partes;

  let header;
  let payload;
  try {
    header = JSON.parse(Buffer.from(headerB64, 'base64url').toString('utf8'));
    payload = JSON.parse(Buffer.from(payloadB64, 'base64url').toString('utf8'));
  } catch {
    // Base64url inválido não lança no Node (caracteres fora do alfabeto são
    // ignorados), então o erro aparece aqui, no JSON.parse.
    throw new TokenInvalido('header ou payload nao e JSON valido');
  }

  if (header === null || typeof header !== 'object' || Array.isArray(header)) {
    throw new TokenInvalido('header nao e um objeto');
  }
  if (payload === null || typeof payload !== 'object' || Array.isArray(payload)) {
    throw new TokenInvalido('payload nao e um objeto');
  }

  return {
    header,
    payload,
    assinatura: Buffer.from(assinaturaB64, 'base64url'),
    // O que foi efetivamente assinado é o texto ORIGINAL dos dois primeiros
    // segmentos. Re-serializar o JSON daria outro byte stream (ordem de chaves,
    // espaços) e a assinatura não fecharia.
    conteudoAssinado: `${headerB64}.${payloadB64}`,
  };
}

/**
 * Confere a assinatura contra o JWKS.
 *
 * Retorna a chave que validou — útil no log para diagnosticar rotação de
 * chaves. Lança `TokenInvalido` se nenhuma validar.
 */
export function verificarAssinatura({ header, assinatura, conteudoAssinado }, jwks) {
  const algoritmoOpenssl = ALGORITMOS_SUPORTADOS[header.alg];
  if (algoritmoOpenssl === undefined) {
    throw new TokenInvalido(`alg nao suportado: ${JSON.stringify(header.alg)}`);
  }

  if (jwks === null || typeof jwks !== 'object' || !Array.isArray(jwks.keys)) {
    throw new TokenInvalido('JWKS malformado: esperado um objeto com a lista "keys"');
  }

  // O Keycloak publica no mesmo JWKS chaves de assinatura (`use: "sig"`) e de
  // criptografia (`use: "enc"`). Tentar verificar com a chave de criptografia
  // falha de um jeito confuso, então ela é filtrada antes.
  const candidatas = jwks.keys.filter((chave) => {
    const serveParaAssinatura = chave.use === undefined || chave.use === 'sig';
    // `kid` ausente no header é permitido pela RFC: nesse caso resta tentar
    // todas as chaves de assinatura.
    const kidCompativel = header.kid === undefined || chave.kid === header.kid;
    return serveParaAssinatura && kidCompativel;
  });

  if (candidatas.length === 0) {
    throw new TokenInvalido(
      `nenhuma chave do JWKS corresponde ao kid ${JSON.stringify(header.kid)}`,
      'kid_desconhecido',
    );
  }

  for (const jwk of candidatas) {
    let chavePublica;
    try {
      chavePublica = createPublicKey({ key: jwk, format: 'jwk' });
    } catch {
      // JWK de tipo incompatível (uma chave EC, por exemplo). Não é erro do
      // token — segue para a próxima candidata.
      continue;
    }

    const verificador = createVerify(algoritmoOpenssl);
    verificador.update(conteudoAssinado);

    let confere = false;
    try {
      confere = verificador.verify(chavePublica, assinatura);
    } catch {
      // Combinação chave/algoritmo inválida (JWK EC com RSA-SHA256). Idem.
      continue;
    }

    if (confere) {
      return jwk;
    }
  }

  // Chave encontrada mas assinatura não fecha: o token é forjado ou corrompido.
  // NÃO é caso de rebuscar o JWKS — fazê-lo transformaria uma enxurrada de
  // tokens inválidos em uma enxurrada de requisições ao Keycloak.
  throw new TokenInvalido('assinatura invalida', 'assinatura');
}

/**
 * Valida as claims que dizem *quando* e *para quem* o token vale.
 *
 * `toleranciaSegundos` absorve dessincronização de relógio entre o Keycloak e a
 * Lambda. Sem ela, um token recém-emitido pode ser recusado por `nbf` no
 * futuro — uma falha intermitente e difícil de reproduzir.
 */
export function validarClaims(payload, {
  issuer,
  audiences = [],
  agoraEmSegundos = Math.floor(Date.now() / 1000),
  toleranciaSegundos = 60,
}) {
  if (typeof issuer !== 'string' || issuer.length === 0) {
    // Erro de configuração, não de token: sem issuer esperado, a validação não
    // significa nada e aceitaria token de qualquer emissor.
    throw new Error('issuer esperado nao foi configurado');
  }

  if (payload.iss !== issuer) {
    throw new TokenInvalido(`issuer inesperado: ${JSON.stringify(payload.iss)}`);
  }

  if (typeof payload.exp !== 'number') {
    // Token sem `exp` nunca expira. Recusar é deliberado: um vazamento desses
    // seria permanente.
    throw new TokenInvalido('token sem claim exp');
  }
  if (agoraEmSegundos > payload.exp + toleranciaSegundos) {
    throw new TokenInvalido('token expirado', 'expirado');
  }

  if (typeof payload.nbf === 'number' && agoraEmSegundos + toleranciaSegundos < payload.nbf) {
    throw new TokenInvalido('token ainda nao e valido (nbf no futuro)');
  }

  // Audiência só é verificada quando configurada.
  //
  // O access token do Keycloak costuma trazer `aud: "account"`, e não o client
  // da API — e o resource server do Spring também não valida audiência
  // (JwtValidators.createDefaultWithIssuer confere apenas o issuer). Exigir
  // audiência aqui por padrão recusaria todo token emitido pelo realm atual.
  if (audiences.length > 0) {
    const doToken = Array.isArray(payload.aud)
      ? payload.aud
      : payload.aud === undefined ? [] : [payload.aud];

    const temIntersecao = doToken.some((aud) => audiences.includes(aud));
    if (!temIntersecao) {
      throw new TokenInvalido(`audiencia inesperada: ${JSON.stringify(payload.aud)}`);
    }
  }

  return payload;
}

/**
 * Extrai as roles no formato do Keycloak — realm_access + resource_access.
 *
 * Replica a leitura que o KeycloakJwtAuthConverter faz no lado Java
 * (src/main/java/.../security/KeycloakJwtAuthConverter.java). A autorização
 * efetiva por role continua sendo da aplicação, com @PreAuthorize: aqui as
 * roles vão apenas para o contexto do authorizer, para aparecerem no log de
 * acesso do API Gateway.
 *
 * Duplicar essa leitura é uma escolha consciente — o alternativa seria o
 * gateway decidir autorização por rota, e aí a regra de acesso viveria em dois
 * lugares que podem divergir.
 */
export function extrairRoles(payload, clientId) {
  const doRealm = payload.realm_access?.roles;
  const doClient = clientId === undefined ? undefined : payload.resource_access?.[clientId]?.roles;

  const todas = [
    ...(Array.isArray(doRealm) ? doRealm : []),
    ...(Array.isArray(doClient) ? doClient : []),
  ];

  const internasDoKeycloak = (role) =>
    role.startsWith('default-roles-') || role === 'offline_access' || role === 'uma_authorization';

  return [...new Set(todas.filter((role) => typeof role === 'string' && !internasDoKeycloak(role)))];
}

/**
 * Identidade do usuário, na mesma ordem de preferência do lado Java: e-mail,
 * depois preferred_username, depois sub.
 *
 * A aplicação identifica cliente e funcionário por E-MAIL, não pelo `sub` do
 * Keycloak — então é o e-mail que precisa aparecer no log para uma requisição
 * ser rastreável até uma pessoa.
 */
export function identificarUsuario(payload) {
  const candidatos = [payload.email, payload.preferred_username, payload.sub];
  return candidatos.find((valor) => typeof valor === 'string' && valor.trim().length > 0) ?? null;
}

export const algoritmosSuportados = Object.keys(ALGORITMOS_SUPORTADOS);
