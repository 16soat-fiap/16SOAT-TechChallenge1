// =============================================================================
// Testes da validação de JWT. Rodar com:  node --test infra/lambda/authorizer
//
// Sem framework e sem dependências: `node:test` e `node:assert` vêm no runtime.
// As chaves são geradas de verdade a cada execução, então os testes exercitam a
// criptografia real em vez de um dublê — é o que dá valor aos casos de
// confusão de algoritmo mais abaixo.
// =============================================================================

import test from 'node:test';
import assert from 'node:assert/strict';
import { createHmac, createSign, generateKeyPairSync } from 'node:crypto';

import {
  TokenInvalido,
  decodificar,
  extrairRoles,
  identificarUsuario,
  validarClaims,
  verificarAssinatura,
} from './jwt.mjs';
import { criarCacheJwks } from './jwks.mjs';

// ─────────────────────────────────────────────────────────────────────────────
// Apoio
// ─────────────────────────────────────────────────────────────────────────────

const ISSUER = 'https://exemplo.execute-api.us-east-1.amazonaws.com/realms/autopecas';

const { publicKey, privateKey } = generateKeyPairSync('rsa', { modulusLength: 2048 });

function jwkPublico({ kid = 'chave-1', use = 'sig' } = {}) {
  return { ...publicKey.export({ format: 'jwk' }), kid, use, alg: 'RS256' };
}

const JWKS = { keys: [jwkPublico()] };

function base64url(objetoOuTexto) {
  const texto = typeof objetoOuTexto === 'string' ? objetoOuTexto : JSON.stringify(objetoOuTexto);
  return Buffer.from(texto, 'utf8').toString('base64url');
}

function payloadValido(extra = {}) {
  const agora = Math.floor(Date.now() / 1000);
  return {
    iss: ISSUER,
    sub: '0cf2b1f4-0000-4000-8000-000000000001',
    exp: agora + 300,
    iat: agora,
    email: 'ana.atendente@autopecas.com.br',
    preferred_username: 'ana.atendente',
    realm_access: { roles: ['ATENDENTE', 'offline_access', 'default-roles-autopecas'] },
    resource_access: { 'autopecas-api': { roles: ['RELATORIOS'] } },
    ...extra,
  };
}

/**
 * Captura a exceção lançada por `fn` e a devolve.
 *
 * `assert.throws` devolve `undefined`, então não serve quando o teste precisa
 * inspecionar um campo do erro — como o `codigo`, que decide se o handler
 * rebusca o JWKS.
 */
function capturar(fn) {
  try {
    fn();
  } catch (erro) {
    return erro;
  }
  assert.fail('esperava uma excecao, nada foi lancado');
}

function assinarRs256(payload, { kid = 'chave-1' } = {}) {
  const cabecalho = base64url({ alg: 'RS256', typ: 'JWT', kid });
  const corpo = base64url(payload);
  const assinatura = createSign('RSA-SHA256').update(`${cabecalho}.${corpo}`).sign(privateKey);
  return `${cabecalho}.${corpo}.${assinatura.toString('base64url')}`;
}

// ─────────────────────────────────────────────────────────────────────────────
// Caminho felizes
// ─────────────────────────────────────────────────────────────────────────────

test('aceita um token legítimo e devolve a chave que validou', () => {
  const decodificado = decodificar(assinarRs256(payloadValido()));
  const chave = verificarAssinatura(decodificado, JWKS);

  assert.equal(chave.kid, 'chave-1');
  assert.doesNotThrow(() => validarClaims(decodificado.payload, { issuer: ISSUER }));
});

test('encontra a chave certa quando o JWKS tem várias', () => {
  const outra = generateKeyPairSync('rsa', { modulusLength: 2048 });
  const jwks = {
    keys: [
      { ...outra.publicKey.export({ format: 'jwk' }), kid: 'chave-antiga', use: 'sig', alg: 'RS256' },
      jwkPublico({ kid: 'chave-nova' }),
    ],
  };

  const decodificado = decodificar(assinarRs256(payloadValido(), { kid: 'chave-nova' }));
  assert.equal(verificarAssinatura(decodificado, jwks).kid, 'chave-nova');
});

test('ignora a chave de criptografia que o Keycloak publica no mesmo JWKS', () => {
  // A chave `enc` vem ANTES da de assinatura, com o mesmo kid do token: se o
  // filtro por `use` não existisse, ela seria escolhida e a validação falharia.
  const jwks = {
    keys: [
      { ...publicKey.export({ format: 'jwk' }), kid: 'chave-1', use: 'enc', alg: 'RSA-OAEP' },
      jwkPublico(),
    ],
  };

  const decodificado = decodificar(assinarRs256(payloadValido()));
  assert.equal(verificarAssinatura(decodificado, jwks).use, 'sig');
});

// ─────────────────────────────────────────────────────────────────────────────
// Confusão de algoritmo — a classe de ataque que mais importa aqui
// ─────────────────────────────────────────────────────────────────────────────

test('recusa token com alg "none" e sem assinatura', () => {
  const token = `${base64url({ alg: 'none', typ: 'JWT' })}.${base64url(payloadValido())}.`;

  assert.throws(
    () => verificarAssinatura(decodificar(token), JWKS),
    (erro) => erro instanceof TokenInvalido && /alg nao suportado/.test(erro.message),
  );
});

test('recusa token HS256 assinado com a chave pública como segredo', () => {
  // O ataque: a chave pública do JWKS é conhecida; o atacante a usa como
  // segredo HMAC e troca o `alg` para HS256. Um validador que escolhesse o
  // algoritmo pelo header aceitaria.
  const segredo = publicKey.export({ format: 'pem', type: 'spki' });
  const cabecalho = base64url({ alg: 'HS256', typ: 'JWT', kid: 'chave-1' });
  const corpo = base64url(payloadValido({ realm_access: { roles: ['ADMIN'] } }));
  const assinatura = createHmac('sha256', segredo).update(`${cabecalho}.${corpo}`).digest('base64url');

  assert.throws(
    () => verificarAssinatura(decodificar(`${cabecalho}.${corpo}.${assinatura}`), JWKS),
    (erro) => erro instanceof TokenInvalido && /alg nao suportado/.test(erro.message),
  );
});

test('recusa payload alterado depois da assinatura', () => {
  const token = assinarRs256(payloadValido());
  const [cabecalho, , assinatura] = token.split('.');
  const corpoForjado = base64url(payloadValido({ realm_access: { roles: ['ADMIN'] } }));

  const erro = capturar(() => verificarAssinatura(decodificar(`${cabecalho}.${corpoForjado}.${assinatura}`), JWKS));

  assert.ok(erro instanceof TokenInvalido);
  // Código "assinatura", e não "kid_desconhecido": é o que impede o handler de
  // rebuscar o JWKS a cada token forjado.
  assert.equal(erro.codigo, 'assinatura');
});

test('sinaliza kid desconhecido com código próprio, para permitir rebuscar o JWKS', () => {
  const decodificado = decodificar(assinarRs256(payloadValido(), { kid: 'chave-rotacionada' }));

  const erro = capturar(() => verificarAssinatura(decodificado, JWKS));

  assert.ok(erro instanceof TokenInvalido);
  assert.equal(erro.codigo, 'kid_desconhecido');
});

// ─────────────────────────────────────────────────────────────────────────────
// Formato
// ─────────────────────────────────────────────────────────────────────────────

test('recusa tokens malformados', () => {
  for (const entrada of ['', 'sem-pontos', 'a.b', 'a.b.c.d', undefined, null, 42]) {
    assert.throws(() => decodificar(entrada), TokenInvalido, `deveria recusar ${JSON.stringify(entrada)}`);
  }
});

test('recusa payload que não é um objeto JSON', () => {
  const token = `${base64url({ alg: 'RS256' })}.${base64url('"apenas-uma-string"')}.x`;
  assert.throws(() => decodificar(token), TokenInvalido);
});

// ─────────────────────────────────────────────────────────────────────────────
// Claims
// ─────────────────────────────────────────────────────────────────────────────

test('recusa issuer diferente do configurado', () => {
  assert.throws(
    () => validarClaims(payloadValido({ iss: 'https://keycloak-do-atacante/realms/autopecas' }), { issuer: ISSUER }),
    (erro) => erro instanceof TokenInvalido && /issuer inesperado/.test(erro.message),
  );
});

test('recusa token expirado, respeitando a tolerância de relógio', () => {
  const agora = Math.floor(Date.now() / 1000);
  const expiradoHa30s = payloadValido({ exp: agora - 30 });

  // Dentro da tolerância de 60s: ainda aceito.
  assert.doesNotThrow(() => validarClaims(expiradoHa30s, { issuer: ISSUER, toleranciaSegundos: 60 }));

  // Sem tolerância: recusado.
  const erro = capturar(() => validarClaims(expiradoHa30s, { issuer: ISSUER, toleranciaSegundos: 0 }));

  assert.ok(erro instanceof TokenInvalido);
  assert.equal(erro.codigo, 'expirado');
});

test('recusa token sem exp — um vazamento desses seria permanente', () => {
  const { exp, ...semExp } = payloadValido();
  assert.throws(() => validarClaims(semExp, { issuer: ISSUER }), /sem claim exp/);
});

test('recusa token com nbf no futuro', () => {
  const agora = Math.floor(Date.now() / 1000);
  assert.throws(
    () => validarClaims(payloadValido({ nbf: agora + 3600 }), { issuer: ISSUER }),
    /ainda nao e valido/,
  );
});

test('só verifica audiência quando ela é configurada', () => {
  // O access token do Keycloak costuma trazer aud: "account".
  const payload = payloadValido({ aud: 'account' });

  assert.doesNotThrow(() => validarClaims(payload, { issuer: ISSUER }));
  assert.doesNotThrow(() => validarClaims(payload, { issuer: ISSUER, audiences: ['account'] }));
  assert.throws(
    () => validarClaims(payload, { issuer: ISSUER, audiences: ['autopecas-api'] }),
    /audiencia inesperada/,
  );
});

test('falha como erro de configuração, não de token, quando o issuer não foi informado', () => {
  const erro = capturar(() => validarClaims(payloadValido(), { issuer: '' }));

  // Importa NÃO ser TokenInvalido: o handler devolveria 403 e culparia o
  // cliente por um erro de deploy nosso.
  assert.ok(erro instanceof Error);
  assert.ok(!(erro instanceof TokenInvalido));
});

// ─────────────────────────────────────────────────────────────────────────────
// Roles e identidade — espelham o KeycloakJwtAuthConverter do lado Java
// ─────────────────────────────────────────────────────────────────────────────

test('combina roles de realm e de client, descartando as internas do Keycloak', () => {
  const roles = extrairRoles(payloadValido(), 'autopecas-api');

  assert.deepEqual(roles.sort(), ['ATENDENTE', 'RELATORIOS']);
});

test('não quebra com token sem nenhuma role', () => {
  const { realm_access, resource_access, ...semRoles } = payloadValido();
  assert.deepEqual(extrairRoles(semRoles, 'autopecas-api'), []);
});

test('identifica o usuário por e-mail, com preferred_username e sub como alternativas', () => {
  assert.equal(identificarUsuario(payloadValido()), 'ana.atendente@autopecas.com.br');

  const { email, ...semEmail } = payloadValido();
  assert.equal(identificarUsuario(semEmail), 'ana.atendente');

  const { preferred_username, ...soSub } = semEmail;
  assert.equal(identificarUsuario(soSub), payloadValido().sub);

  assert.equal(identificarUsuario({}), null);
});

// ─────────────────────────────────────────────────────────────────────────────
// Cache do JWKS
// ─────────────────────────────────────────────────────────────────────────────

function respostaOk(corpo) {
  return { ok: true, status: 200, json: async () => corpo };
}

test('busca o JWKS uma vez e serve do cache enquanto o TTL vale', async () => {
  let chamadas = 0;
  const cache = criarCacheJwks({
    url: 'https://exemplo/jwks',
    ttlSegundos: 600,
    buscar: async () => {
      chamadas += 1;
      return respostaOk(JWKS);
    },
  });

  await cache.obter();
  await cache.obter();
  await cache.obter();

  assert.equal(chamadas, 1);
});

test('rebusca depois do TTL', async () => {
  let chamadas = 0;
  let relogio = 0;
  const cache = criarCacheJwks({
    url: 'https://exemplo/jwks',
    ttlSegundos: 10,
    buscar: async () => {
      chamadas += 1;
      return respostaOk(JWKS);
    },
    agora: () => relogio,
  });

  await cache.obter();
  relogio += 11_000;
  await cache.obter();

  assert.equal(chamadas, 2);
});

test('limita atualizações forçadas, para uma rajada de tokens inválidos não virar rajada no Keycloak', async () => {
  let chamadas = 0;
  let relogio = 0;
  const cache = criarCacheJwks({
    url: 'https://exemplo/jwks',
    ttlSegundos: 600,
    intervaloMinimoForcado: 30,
    buscar: async () => {
      chamadas += 1;
      return respostaOk(JWKS);
    },
    agora: () => relogio,
  });

  await cache.obter();
  assert.equal(chamadas, 1);

  await cache.obter({ forcarAtualizacao: true });
  assert.equal(chamadas, 2, 'a primeira forçada deve passar');

  relogio += 5_000;
  await cache.obter({ forcarAtualizacao: true });
  assert.equal(chamadas, 2, 'dentro do intervalo mínimo, deve servir do cache');

  relogio += 31_000;
  await cache.obter({ forcarAtualizacao: true });
  assert.equal(chamadas, 3, 'passado o intervalo, deve rebuscar');
});

test('serve o cache vencido quando o Keycloak está fora — a API não cai com ele', async () => {
  let chamadas = 0;
  let relogio = 0;
  const cache = criarCacheJwks({
    url: 'https://exemplo/jwks',
    ttlSegundos: 10,
    buscar: async () => {
      chamadas += 1;
      if (chamadas === 1) return respostaOk(JWKS);
      throw new Error('conexao recusada');
    },
    agora: () => relogio,
  });

  const primeira = await cache.obter();
  relogio += 11_000;
  const segunda = await cache.obter();

  assert.equal(chamadas, 2);
  assert.deepEqual(segunda, primeira);
});

test('propaga o erro quando falha sem ter nada em cache', async () => {
  const cache = criarCacheJwks({
    url: 'https://exemplo/jwks',
    buscar: async () => {
      throw new Error('conexao recusada');
    },
  });

  await assert.rejects(() => cache.obter(), /falha ao buscar o JWKS/);
});

test('ignora um JWKS vazio em vez de substituir um cache bom por um inútil', async () => {
  const cache = criarCacheJwks({
    url: 'https://exemplo/jwks',
    buscar: async () => respostaOk({ keys: [] }),
  });

  await assert.rejects(() => cache.obter(), /sem chaves/);
});

test('trata HTTP de erro como falha', async () => {
  const cache = criarCacheJwks({
    url: 'https://exemplo/jwks',
    buscar: async () => ({ ok: false, status: 503, json: async () => ({}) }),
  });

  await assert.rejects(() => cache.obter(), /HTTP 503/);
});

test('uma única busca em voo quando duas validações chegam com o cache vencido', async () => {
  let chamadas = 0;
  const cache = criarCacheJwks({
    url: 'https://exemplo/jwks',
    buscar: async () => {
      chamadas += 1;
      await new Promise((resolve) => setTimeout(resolve, 10));
      return respostaOk(JWKS);
    },
  });

  await Promise.all([cache.obter(), cache.obter(), cache.obter()]);

  assert.equal(chamadas, 1);
});
