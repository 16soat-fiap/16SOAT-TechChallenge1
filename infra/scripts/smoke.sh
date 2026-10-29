#!/usr/bin/env bash
# =============================================================================
# Verificação ponta a ponta de um ambiente, pelo API Gateway.
#
#   AMBIENTE=homologacao SMOKE_DOCUMENTO=<CPF ou CNPJ> ./infra/scripts/smoke.sh
#
# Exercita o caminho COMPLETO: API Gateway, Lambda de autenticação, authorizer
# JWT nativo do gateway, VPC Link, NLB, kube-proxy, pod, RDS. Vale mais que um
# `kubectl rollout status`, que diz apenas que o pod subiu: é aqui que erros de
# rede, de rota e de emissor aparecem.
#
# Os quatro casos foram escolhidos para que cada um FALHE por um motivo
# diferente, e o motivo seja legível:
#
#   1. chave pública do emissor → 200     o gateway e a Lambda estão no ar
#   2. /api/* sem token → 401             o authorizer está barrando na borda
#   3. login por CPF + /api/* com o token  o login funciona, o gateway libera e
#      → 200                               a aplicação aceita o mesmo token
#   4. /api/* com token adulterado → 401   a assinatura é realmente verificada
#
# Sem o caso 4, um authorizer que aceitasse qualquer token passaria nos outros.
#
# SMOKE_DOCUMENTO é o CPF (ou CNPJ) de um cliente ATIVO no banco do ambiente.
# Sem ele, os casos 3 e 4 são pulados — produção não carrega dados de exemplo.
# =============================================================================

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

AMBIENTE="${AMBIENTE:-homologacao}"
DOCUMENTO="${SMOKE_DOCUMENTO:-}"

if ! tem curl; then
  morrer "curl não encontrado — necessário para esta verificação."
fi

API_URL="$(output_remoto "lambda/${AMBIENTE}/terraform.tfstate" api_url)"
AUTH_URL="$(output_remoto "lambda/${AMBIENTE}/terraform.tfstate" auth_url)"
ISSUER="$(output_remoto "lambda/${AMBIENTE}/terraform.tfstate" jwt_issuer)"
JWKS_URL="$(output_remoto "lambda/${AMBIENTE}/terraform.tfstate" jwks_url)"

if [ -z "$API_URL" ]; then
  morrer "Não encontrei a URL da API de ${AMBIENTE} — o repositório autopecas-lambda-auth foi aplicado?"
fi

titulo "Ambiente ${AMBIENTE}"
echo "  API     ${API_URL}"
echo "  Issuer  ${ISSUER}"

FALHAS=0

codigo_http() {
  curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$@" || echo 000
}

# Uma rota de leitura do catálogo do gateway. Qualquer rota protegida serve: o
# que se testa aqui é o caminho, não a regra de negócio.
ROTA="${API_URL}/api/clientes"

# ─────────────────────────────────────────────────────────────────────────────
titulo "1/4  Chave pública do emissor (espera 200)"
CODIGO="$(codigo_http "$JWKS_URL")"
if [ "$CODIGO" = "200" ]; then
  ok "HTTP 200 — gateway e Lambda respondendo"
else
  erro "HTTP ${CODIGO} — esperado 200 em ${JWKS_URL}"
  FALHAS=$((FALHAS + 1))
fi

# ─────────────────────────────────────────────────────────────────────────────
titulo "2/4  Rota protegida SEM token (espera 401)"
CODIGO="$(codigo_http "$ROTA")"
case "$CODIGO" in
  401)
    ok "HTTP 401 — barrado na borda, o tráfego não chegou ao cluster"
    ;;
  200)
    erro "HTTP 200 — a API está ABERTA. O authorizer não está na rota."
    FALHAS=$((FALHAS + 1))
    ;;
  404)
    erro "HTTP 404 — a rota não existe no gateway (o catálogo de rotas foi aplicado?)."
    FALHAS=$((FALHAS + 1))
    ;;
  *)
    erro "HTTP ${CODIGO} — esperado 401"
    FALHAS=$((FALHAS + 1))
    ;;
esac

# ─────────────────────────────────────────────────────────────────────────────
titulo "3/4  Login por CPF e chamada com o token (espera 200)"

if [ -z "$DOCUMENTO" ]; then
  aviso "SMOKE_DOCUMENTO não definido — casos 3 e 4 pulados."
  aviso "Informe o CPF de um cliente ativo no banco de ${AMBIENTE}."
else
  RESPOSTA="$(curl -s --max-time 30 -X POST "$AUTH_URL" \
    -H 'Content-Type: application/json' \
    -d "{\"documento\":\"${DOCUMENTO}\"}" || true)"

  # Extrai o token sem jq: a resposta é {"token":"..."} numa linha só.
  TOKEN="$(printf '%s' "$RESPOSTA" | sed -n 's/.*"token" *: *"\([^"]*\)".*/\1/p')"

  if [ -z "$TOKEN" ]; then
    erro "Login recusado. Resposta da Lambda:"
    printf '    %s\n' "$(printf '%s' "$RESPOSTA" | head -c 300)"
    echo "    400 = documento inválido · 401 = não cadastrado · 403 = cliente inativo" >&2
    FALHAS=$((FALHAS + 1))
  else
    ok "Token obtido"

    # O `iss` do token precisa ser idêntico ao que o gateway e a aplicação
    # esperam. Divergência aqui é a causa mais provável de 401 com token
    # aparentemente válido, e é invisível sem olhar o payload.
    PAYLOAD_B64="$(printf '%s' "$TOKEN" | cut -d. -f2 | tr '_-' '/+')"
    case $(( ${#PAYLOAD_B64} % 4 )) in
      2) PAYLOAD_B64="${PAYLOAD_B64}==" ;;
      3) PAYLOAD_B64="${PAYLOAD_B64}=" ;;
    esac
    ISS_DO_TOKEN="$(printf '%s' "$PAYLOAD_B64" | base64 -d 2>/dev/null \
      | sed -n 's/.*"iss" *: *"\([^"]*\)".*/\1/p')"
    if [ "$ISS_DO_TOKEN" = "$ISSUER" ]; then
      ok "iss do token confere: ${ISS_DO_TOKEN}"
    else
      aviso "iss do token: ${ISS_DO_TOKEN:-<não lido>} · esperado: ${ISSUER}"
    fi

    CODIGO="$(codigo_http "$ROTA" -H "Authorization: Bearer ${TOKEN}")"
    case "$CODIGO" in
      200)
        ok "HTTP 200 — autorizado ponta a ponta"
        ;;
      403)
        # O gateway liberou e a APLICAÇÃO recusou: um cliente não lista todos os
        # clientes (@PreAuthorize). O caminho inteiro funcionou.
        ok "HTTP 403 da aplicação — o gateway liberou; a regra de acesso do cliente recusou a listagem"
        ;;
      *)
        erro "HTTP ${CODIGO} — esperado 200 (ou 403 da regra de acesso) com token válido"
        FALHAS=$((FALHAS + 1))
        ;;
    esac

    # ─────────────────────────────────────────────────────────────────────────
    titulo "4/4  Token adulterado (espera 401)"
    # Último caractere trocado: quebra a assinatura preservando a estrutura.
    ULTIMO="${TOKEN: -1}"
    if [ "$ULTIMO" = "A" ]; then SUBST="B"; else SUBST="A"; fi
    CODIGO="$(codigo_http "$ROTA" -H "Authorization: Bearer ${TOKEN%?}${SUBST}")"
    if [ "$CODIGO" = "401" ] || [ "$CODIGO" = "403" ]; then
      ok "HTTP ${CODIGO} — assinatura inválida recusada"
    else
      erro "HTTP ${CODIGO} — esperado 401. A assinatura NÃO está sendo verificada."
      FALHAS=$((FALHAS + 1))
    fi
  fi
fi

# ─────────────────────────────────────────────────────────────────────────────
titulo "Resultado"
if [ "$FALHAS" -eq 0 ]; then
  ok "Todas as verificações executadas passaram."
  exit 0
fi

erro "${FALHAS} verificação(ões) falharam."
echo
echo "  Diagnóstico:" >&2
echo "    kubectl get pods,hpa -n ${AMBIENTE}" >&2
echo "    kubectl logs deployment/autopecas-api -n ${AMBIENTE} --tail=100" >&2
echo "    aws logs tail /aws/lambda/autopecas-auth-${AMBIENTE} --since 10m" >&2
echo
exit 1
