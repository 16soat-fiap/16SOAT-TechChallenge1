#!/usr/bin/env bash
# =============================================================================
# Verificação ponta a ponta, pelo API Gateway.
#
#   ./infra/scripts/smoke.sh
#
# Exercita o caminho COMPLETO — gateway, Lambda authorizer, VPC Link, NLB,
# kube-proxy, pod, RDS. Vale mais que um `kubectl rollout status`, que diz
# apenas que o pod subiu: é aqui que erros de rede e de issuer aparecem.
#
# Os quatro casos foram escolhidos para que cada um FALHE por um motivo
# diferente, e o motivo seja legível:
#
#   1. /health sem token devolve 200      → o caminho de rede inteiro funciona
#   2. /api/* sem token devolve 401/403   → o authorizer está barrando
#   3. /api/* com token válido devolve 200 → o authorizer está liberando, e o
#                                            `iss` concorda nas três pontas
#   4. /api/* com token adulterado → 403   → a validação de assinatura funciona
#
# Sem o caso 4, um authorizer que devolvesse `isAuthorized: true` para tudo
# passaria nos outros três.
# =============================================================================

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

USUARIO="${KEYCLOAK_USER:-test_user}"
SENHA="${KEYCLOAK_PASSWORD:-1234}"

if ! tem curl; then
  morrer "curl não encontrado — necessário para esta verificação."
fi

API_URL="$(tf_output 04-gateway-auth api_base_url)"
ISSUER="$(tf_output 04-gateway-auth issuer)"

# O client do realm. É public e tem directAccessGrants habilitado (conferido em
# keycloak/realm-export.json), então o grant password dispensa client_secret.
CLIENT_ID="${KEYCLOAK_CLIENT_ID:-autopecas-api}"

if [ -z "$API_URL" ]; then
  erro "Não encontrei a URL da API."
  echo "  Rode primeiro:  ./infra/scripts/up.sh" >&2
  exit 1
fi

titulo "Ambiente"
echo "  API     ${API_URL}"
echo "  Issuer  ${ISSUER}"

FALHAS=0

codigo_http() {
  curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$@" || echo 000
}

# ─────────────────────────────────────────────────────────────────────────────
titulo "1/4  Health check sem token (espera 200)"
# Com retentativa: num cluster recém-criado o alvo pode ainda estar entrando no
# target group do NLB, o que leva ~30s depois de o pod ficar ready.
CODIGO=000
for tentativa in $(seq 1 20); do
  CODIGO="$(codigo_http "${API_URL}/health")"
  if [ "$CODIGO" = "200" ]; then break; fi
  printf '    tentativa %s/20: HTTP %s\n' "$tentativa" "$CODIGO"
  sleep 15
done

if [ "$CODIGO" = "200" ]; then
  ok "HTTP 200 — gateway, VPC Link, NLB e pod respondendo"
else
  erro "HTTP ${CODIGO} — esperado 200"
  FALHAS=$((FALHAS + 1))
fi

# ─────────────────────────────────────────────────────────────────────────────
titulo "2/4  Endpoint protegido SEM token (espera 401 ou 403)"
CODIGO="$(codigo_http "${API_URL}/api/clientes")"
case "$CODIGO" in
  401|403)
    ok "HTTP ${CODIGO} — barrado na borda, o tráfego não chegou ao cluster"
    ;;
  200)
    erro "HTTP 200 — a API está ABERTA. O authorizer não está na rota."
    FALHAS=$((FALHAS + 1))
    ;;
  404)
    erro "HTTP 404 — a rota /api/{proxy+} não existe no gateway."
    FALHAS=$((FALHAS + 1))
    ;;
  *)
    erro "HTTP ${CODIGO} — esperado 401 ou 403"
    FALHAS=$((FALHAS + 1))
    ;;
esac

# ─────────────────────────────────────────────────────────────────────────────
titulo "3/4  Obtendo token no Keycloak e chamando com ele (espera 200)"

RESPOSTA="$(curl -s --max-time 30 -X POST \
  "${ISSUER}/protocol/openid-connect/token" \
  -d grant_type=password \
  -d "client_id=${CLIENT_ID}" \
  -d "username=${USUARIO}" \
  -d "password=${SENHA}" || true)"

# Extrai o access_token sem jq: o campo vem numa única linha de JSON.
TOKEN="$(printf '%s' "$RESPOSTA" \
  | sed -n 's/.*"access_token" *: *"\([^"]*\)".*/\1/p')"

if [ -z "$TOKEN" ]; then
  erro "Não obtive token. Resposta do Keycloak:"
  printf '    %s\n' "$(printf '%s' "$RESPOSTA" | head -c 400)"
  echo
  echo "    Causas prováveis:" >&2
  echo "      • sslRequired=\"external\" no realm e o X-Forwarded-Proto não chegou" >&2
  echo "      • usuário/senha diferentes (ajuste com KEYCLOAK_USER e KEYCLOAK_PASSWORD)" >&2
  echo "      • o Keycloak ainda está subindo (o primeiro boot leva minutos)" >&2
  FALHAS=$((FALHAS + 1))
else
  ok "Token obtido"

  # O `iss` do token precisa ser idêntico ao que a Lambda e o Spring esperam.
  # Divergência aqui é a causa mais provável de 403 com token aparentemente
  # válido, e é invisível sem olhar o payload.
  PAYLOAD_B64="$(printf '%s' "$TOKEN" | cut -d. -f2)"
  # base64url → base64, com o padding que o decodificador exige.
  PAYLOAD_B64="$(printf '%s' "$PAYLOAD_B64" | tr '_-' '/+')"
  case $(( ${#PAYLOAD_B64} % 4 )) in
    2) PAYLOAD_B64="${PAYLOAD_B64}==" ;;
    3) PAYLOAD_B64="${PAYLOAD_B64}=" ;;
  esac
  ISS_DO_TOKEN="$(printf '%s' "$PAYLOAD_B64" | base64 -d 2>/dev/null \
    | sed -n 's/.*"iss" *: *"\([^"]*\)".*/\1/p')"

  if [ "$ISS_DO_TOKEN" = "$ISSUER" ]; then
    ok "iss do token confere: ${ISS_DO_TOKEN}"
  else
    aviso "iss do token:   ${ISS_DO_TOKEN:-<não lido>}"
    aviso "iss esperado:   ${ISSUER}"
    aviso "Divergência aqui faz a Lambda devolver 403 com token legítimo."
  fi

  CODIGO="$(codigo_http "${API_URL}/api/clientes" -H "Authorization: Bearer ${TOKEN}")"
  if [ "$CODIGO" = "200" ]; then
    ok "HTTP 200 — autorizado ponta a ponta"
  else
    erro "HTTP ${CODIGO} — esperado 200 com token válido"
    echo "    Log da Lambda:  aws logs tail /aws/lambda/autopecas-authorizer --since 5m" >&2
    FALHAS=$((FALHAS + 1))
  fi

  # ───────────────────────────────────────────────────────────────────────────
  titulo "4/4  Token adulterado (espera 403)"
  # Último caractere trocado: quebra a assinatura preservando a estrutura. Um
  # authorizer que só confere a PRESENÇA do header passaria nos testes 1 a 3 e
  # falha aqui.
  ULTIMO="${TOKEN: -1}"
  if [ "$ULTIMO" = "A" ]; then SUBST="B"; else SUBST="A"; fi
  TOKEN_FALSO="${TOKEN%?}${SUBST}"

  CODIGO="$(codigo_http "${API_URL}/api/clientes" -H "Authorization: Bearer ${TOKEN_FALSO}")"
  if [ "$CODIGO" = "403" ] || [ "$CODIGO" = "401" ]; then
    ok "HTTP ${CODIGO} — assinatura inválida recusada"
  else
    erro "HTTP ${CODIGO} — esperado 403. A assinatura NÃO está sendo verificada."
    FALHAS=$((FALHAS + 1))
  fi
fi

# ─────────────────────────────────────────────────────────────────────────────
titulo "Resultado"
if [ "$FALHAS" -eq 0 ]; then
  ok "Todas as verificações passaram."
  echo
  echo "  Swagger:  ${API_URL}/swagger-ui.html"
  echo
  exit 0
fi

erro "${FALHAS} verificação(ões) falharam."
echo
echo "  Diagnóstico:" >&2
echo "    kubectl get pods,hpa -n autopecas" >&2
echo "    kubectl logs deployment/autopecas-api -n autopecas --tail=100" >&2
echo "    aws logs tail /aws/lambda/autopecas-authorizer --since 10m" >&2
echo
exit 1
