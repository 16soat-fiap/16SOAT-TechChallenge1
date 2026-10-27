// =============================================================================
// Lambda Authorizer do API Gateway — a "function serverless de autenticação".
//
// O que ela faz: intercepta TODA requisição para /api/* antes de o tráfego
// entrar na VPC, e só libera quem apresenta um JWT válido emitido pelo
// Keycloak. Quem não apresenta recebe 403 na borda — sem consumir nó do
// cluster, sem abrir conexão no banco, sem aparecer no log da aplicação.
//
// O que ela NÃO faz: emitir token. O emissor continua sendo o Keycloak, e a
// rota /realms/* do gateway passa por ele direto, sem authorizer — teria que
// haver um token para pedir um token.
//
// A aplicação Spring REVALIDA o mesmo token (oauth2ResourceServer em
// config/SecurityConfig.java). A redundância é intencional: o authorizer
// protege o perímetro, e a validação no serviço garante que uma falha de
// configuração no gateway não vire acesso irrestrito. Além disso, a autorização
// por role (@PreAuthorize) precisa das claims dentro da aplicação de todo jeito.
// =============================================================================

import {
  TokenInvalido,
  decodificar,
  extrairRoles,
  identificarUsuario,
  validarClaims,
  verificarAssinatura,
} from './jwt.mjs';
import { criarCacheJwks } from './jwks.mjs';

// Lido uma vez por container, no cold start — e não por invocação. Um erro de
// configuração falha imediatamente, no primeiro boot, em vez de só na primeira
// requisição.
const config = Object.freeze({
  jwksUrl: obrigatoria('JWKS_URL'),
  issuer: obrigatoria('ISSUER'),
  clientId: process.env.CLIENT_ID,
  audiences: (process.env.AUDIENCES ?? '')
    .split(',')
    .map((valor) => valor.trim())
    .filter((valor) => valor.length > 0),
  ttlJwksSegundos: Number(process.env.JWKS_TTL_SECONDS ?? 600),
  toleranciaRelogioSegundos: Number(process.env.CLOCK_SKEW_SECONDS ?? 60),
});

const cacheJwks = criarCacheJwks({
  url: config.jwksUrl,
  ttlSegundos: config.ttlJwksSegundos,
});

function obrigatoria(nome) {
  const valor = process.env[nome];
  if (typeof valor !== 'string' || valor.trim().length === 0) {
    throw new Error(`variavel de ambiente ${nome} e obrigatoria`);
  }
  return valor.trim();
}

/**
 * Extrai o token do header Authorization.
 *
 * `identitySource` é preenchido pelo API Gateway com o valor exato das fontes
 * declaradas em `identity_sources` (ver 04-gateway-auth/authorizer.tf) — é a
 * fonte primária. O fallback por header existe porque o payload 2.0 normaliza
 * nomes de header para minúsculas, e um teste manual com `aws lambda invoke`
 * não traz `identitySource`.
 */
function extrairToken(evento) {
  const bruto = evento?.identitySource?.[0] ?? evento?.headers?.authorization;

  if (typeof bruto !== 'string' || bruto.trim().length === 0) {
    throw new TokenInvalido('header Authorization ausente', 'ausente');
  }

  const partes = bruto.trim().split(/\s+/);
  if (partes.length !== 2 || partes[0].toLowerCase() !== 'bearer') {
    throw new TokenInvalido('header Authorization nao esta no formato "Bearer <token>"', 'formato');
  }

  return partes[1];
}

/**
 * Log estruturado, em JSON de uma linha.
 *
 * Nunca registra o token nem a assinatura: o CloudWatch Logs é legível por
 * quem tem permissão de leitura de log, que é bem mais gente do que deveria
 * poder se passar por um usuário. O que se registra é o suficiente para
 * diagnosticar — motivo, rota, e quem era quando se sabe.
 */
function registrar(nivel, dados) {
  const linha = JSON.stringify({ nivel, ...dados });
  if (nivel === 'erro') {
    console.error(linha);
  } else {
    console.log(linha);
  }
}

export const handler = async (evento) => {
  const rota = evento?.routeKey ?? evento?.rawPath ?? 'desconhecida';
  const requestId = evento?.requestContext?.requestId;

  let token;
  try {
    token = extrairToken(evento);
  } catch (erro) {
    registrar('aviso', { evento: 'negado', motivo: erro.message, codigo: erro.codigo, rota, requestId });
    return { isAuthorized: false };
  }

  let decodificado;
  try {
    decodificado = decodificar(token);
  } catch (erro) {
    registrar('aviso', { evento: 'negado', motivo: erro.message, codigo: erro.codigo, rota, requestId });
    return { isAuthorized: false };
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Assinatura.
  //
  // A segunda tentativa cobre rotação de chave: o Keycloak passou a assinar com
  // um `kid` que o cache ainda não tem. Só o código `kid_desconhecido` dispara
  // a rebusca — assinatura que não fecha é token forjado, e rebuscar o JWKS a
  // cada um desses seria um amplificador de tráfego contra o Keycloak.
  // ───────────────────────────────────────────────────────────────────────────
  let jwks;
  try {
    jwks = await cacheJwks.obter();
  } catch (erro) {
    // Falha de INFRAESTRUTURA, não de credencial. Propagar faz o API Gateway
    // responder 500; devolver `isAuthorized: false` daria 403 e diria ao
    // cliente que o token dele é inválido, quando o problema é nosso.
    registrar('erro', { evento: 'jwks_indisponivel', motivo: erro.message, rota, requestId });
    throw erro;
  }

  try {
    try {
      verificarAssinatura(decodificado, jwks);
    } catch (erro) {
      if (!(erro instanceof TokenInvalido) || erro.codigo !== 'kid_desconhecido') {
        throw erro;
      }
      registrar('info', { evento: 'jwks_reconsultado', kid: decodificado.header.kid, requestId });
      jwks = await cacheJwks.obter({ forcarAtualizacao: true });
      verificarAssinatura(decodificado, jwks);
    }

    validarClaims(decodificado.payload, {
      issuer: config.issuer,
      audiences: config.audiences,
      toleranciaSegundos: config.toleranciaRelogioSegundos,
    });
  } catch (erro) {
    if (erro instanceof TokenInvalido) {
      registrar('aviso', { evento: 'negado', motivo: erro.message, codigo: erro.codigo, rota, requestId });
      return { isAuthorized: false };
    }
    registrar('erro', { evento: 'falha_inesperada', motivo: erro.message, rota, requestId });
    throw erro;
  }

  const usuario = identificarUsuario(decodificado.payload);
  const roles = extrairRoles(decodificado.payload, config.clientId);

  registrar('info', { evento: 'autorizado', usuario, roles, rota, requestId });

  return {
    isAuthorized: true,
    // O contexto vai para o log de acesso do API Gateway (ver a
    // `access_log_settings` do stage), tornando cada requisição rastreável até
    // um usuário sem precisar cruzar log do gateway com log da aplicação.
    //
    // Ele NÃO é repassado como header para a integração privada — isso exigiria
    // parameter mapping explícito —, e é melhor assim: a aplicação deve confiar
    // no token que ela mesma valida, não num header posto pelo gateway.
    context: {
      usuario: usuario ?? '',
      roles: roles.join(','),
      sub: decodificado.payload.sub ?? '',
    },
  };
};
