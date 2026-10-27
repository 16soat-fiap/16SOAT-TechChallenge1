// =============================================================================
// Cache do JWKS do Keycloak.
//
// Por que existe: sem cache, CADA requisição autorizada custaria uma chamada
// HTTP ao Keycloak antes de qualquer validação. Isso somaria latência a todo
// request, faria o Keycloak virar o gargalo da API inteira e, pior, tornaria
// uma indisponibilidade dele numa indisponibilidade total — mesmo com o cluster
// saudável.
//
// O cache vive na memória do container da Lambda, então sobrevive entre
// invocações do mesmo container e morre com ele. Combinado com o cache de
// resultado do próprio authorizer no API Gateway (300s), a frequência real de
// chamadas ao Keycloak fica muito baixa.
// =============================================================================

/**
 * @param {object} opcoes
 * @param {string} opcoes.url                      endpoint JWKS do Keycloak
 * @param {number} [opcoes.ttlSegundos]            validade do cache
 * @param {number} [opcoes.intervaloMinimoForcado] piso entre atualizações forçadas
 * @param {number} [opcoes.timeoutMs]              teto de espera pela resposta
 * @param {Function} [opcoes.buscar]               injeção de fetch, para teste
 * @param {Function} [opcoes.agora]                injeção de relógio, para teste
 */
export function criarCacheJwks({
  url,
  ttlSegundos = 600,
  intervaloMinimoForcado = 30,
  timeoutMs = 3000,
  buscar = fetch,
  agora = () => Date.now(),
}) {
  if (typeof url !== 'string' || url.length === 0) {
    throw new Error('URL do JWKS nao configurada');
  }

  let emCache = null;
  let expiraEm = 0;
  // -Infinity, e não 0: com 0, a PRIMEIRA atualização forçada seria barrada
  // pelo intervalo mínimo sempre que o relógio ainda estivesse próximo da
  // origem — e a rotação de chave do Keycloak só seria percebida no TTL
  // seguinte. É o estado "nunca houve busca forçada", não "houve no instante 0".
  let ultimaBuscaForcada = Number.NEGATIVE_INFINITY;
  // Uma única busca em voo por vez: se duas validações chegarem com o cache
  // vencido, ambas esperam a MESMA promessa em vez de disparar duas chamadas.
  let emVoo = null;

  async function buscarDoKeycloak() {
    let resposta;
    try {
      resposta = await buscar(url, {
        // Sem timeout, uma Lambda com o Keycloak lento fica pendurada até o
        // timeout da própria função — e o API Gateway devolve 504 para o
        // cliente em vez de um erro imediato.
        signal: AbortSignal.timeout(timeoutMs),
        headers: { accept: 'application/json' },
      });
    } catch (causa) {
      throw new Error(`falha ao buscar o JWKS em ${url}`, { cause: causa });
    }

    if (!resposta.ok) {
      throw new Error(`JWKS respondeu HTTP ${resposta.status}`);
    }

    const corpo = await resposta.json();
    if (corpo === null || typeof corpo !== 'object' || !Array.isArray(corpo.keys) || corpo.keys.length === 0) {
      // Um JWKS vazio substituiria um cache bom por um inútil, e aí TODA
      // requisição passaria a falhar com "kid desconhecido".
      throw new Error('JWKS sem chaves — resposta ignorada');
    }

    return corpo;
  }

  return {
    /**
     * Devolve o JWKS, do cache quando válido.
     *
     * `forcarAtualizacao` serve ao caso de rotação de chave: o Keycloak passou a
     * assinar com um `kid` novo e o cache ainda tem o antigo. É limitado por
     * `intervaloMinimoForcado` para que uma rajada de tokens inválidos não se
     * transforme em uma rajada de chamadas ao Keycloak.
     */
    async obter({ forcarAtualizacao = false } = {}) {
      const momento = agora();
      const cacheValido = emCache !== null && momento < expiraEm;

      if (cacheValido && !forcarAtualizacao) {
        return emCache;
      }

      if (cacheValido && forcarAtualizacao) {
        const desdeUltimaForcada = (momento - ultimaBuscaForcada) / 1000;
        if (desdeUltimaForcada < intervaloMinimoForcado) {
          return emCache;
        }
        ultimaBuscaForcada = momento;
      }

      if (emVoo !== null) {
        return emVoo;
      }

      emVoo = buscarDoKeycloak()
        .then((jwks) => {
          emCache = jwks;
          expiraEm = agora() + ttlSegundos * 1000;
          return jwks;
        })
        .finally(() => {
          emVoo = null;
        });

      try {
        return await emVoo;
      } catch (erro) {
        // Cache expirado ainda é melhor que nada: as chaves do Keycloak duram
        // muito mais que o TTL deste cache, então servir o conteúdo vencido
        // mantém a API de pé durante uma indisponibilidade do Keycloak. Sem
        // isto, um Keycloak reiniciando derrubaria toda a autorização.
        if (emCache !== null) {
          console.warn(JSON.stringify({
            evento: 'jwks_servido_do_cache_vencido',
            motivo: erro.message,
          }));
          return emCache;
        }
        throw erro;
      }
    },
  };
}
