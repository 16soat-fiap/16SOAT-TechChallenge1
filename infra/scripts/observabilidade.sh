#!/usr/bin/env bash
# =============================================================================
# Cria (ou atualiza) no New Relic os dashboards, os alertas e o monitor de
# disponibilidade de cada ambiente — a stack infra/newrelic.
#
#   NEW_RELIC_API_KEY=NRAK-...  NEW_RELIC_ACCOUNT_ID=1234567 \
#   NEW_RELIC_EMAIL_ALERTAS=voce@exemplo.com  ./infra/scripts/observabilidade.sh
#
# Opcionais:
#   NEW_RELIC_REGION   US (padrão) ou EU
#   APROVAR_AUTO=sim   aplica sem pedir confirmação (CI)
#   DESTRUIR=sim       remove tudo o que a stack criou no New Relic
#
# NEW_RELIC_API_KEY é a User API key (NRAK-...), não a license key: a license
# key só ENVIA dados (agente, cluster); a User key administra a conta. Ela fica
# só no ambiente — o provider a lê de lá — e nunca vai para arquivo.
#
# O monitor de disponibilidade de cada ambiente aponta para
# <api_url do gateway>/actuator/health, lido do state do repositório
# autopecas-lambda-auth. Ambiente sem gateway de pé fica sem monitor (e sem o
# alerta de healthcheck) até a próxima execução — o resto é criado igual.
#
# Rodar de novo depois de cada subida do ambiente: o gateway do Learner Lab
# ganha URL nova a cada vez, e o monitor precisa acompanhar.
# =============================================================================

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

STACK_REL="infra/newrelic"
APROVAR_AUTO="${APROVAR_AUTO:-nao}"
DESTRUIR="${DESTRUIR:-nao}"
NEW_RELIC_REGION="${NEW_RELIC_REGION:-US}"

for var in NEW_RELIC_API_KEY NEW_RELIC_ACCOUNT_ID NEW_RELIC_EMAIL_ALERTAS; do
  if [ -z "${!var:-}" ]; then
    morrer "${var} não definida (ver o cabeçalho deste script)."
  fi
done
case "$NEW_RELIC_API_KEY" in
  NRAK-*) ;;
  *) morrer "NEW_RELIC_API_KEY deve ser a User API key (começa com NRAK-), não a license key." ;;
esac

exigir_docker
exigir_aws_configurada

titulo "1/3  Lendo os ambientes"
BUCKET="$(bucket_state)"
REGIAO="$(regiao)"

AMBIENTES_TF=""
for amb in homologacao producao; do
  api_url="$(output_remoto "lambda/${amb}/terraform.tfstate" api_url)"
  if [ -n "$api_url" ]; then
    AMBIENTES_TF+="  ${amb} = { health_url = \"${api_url%/}/actuator/health\" }"$'\n'
    ok "${amb}: monitor em ${api_url%/}/actuator/health"
  else
    AMBIENTES_TF+="  ${amb} = {}"$'\n'
    aviso "${amb}: gateway não encontrado — dashboard e alertas sim, monitor de disponibilidade não"
  fi
done

# Gerados e ignorados pelo git (infra/newrelic/.gitignore): o bucket identifica a
# conta AWS de quem roda, e o e-mail é dado pessoal.
cat > "${REPO_ROOT}/${STACK_REL}/backend.hcl" <<EOF
bucket = "${BUCKET}"
region = "${REGIAO}"
EOF

cat > "${REPO_ROOT}/${STACK_REL}/terraform.tfvars" <<EOF
# GERADO por infra/scripts/observabilidade.sh — não versionado, não edite à mão.
account_id    = ${NEW_RELIC_ACCOUNT_ID}
region        = "${NEW_RELIC_REGION}"
email_alertas = "${NEW_RELIC_EMAIL_ALERTAS}"

ambientes = {
${AMBIENTES_TF}}
EOF
ok "backend.hcl e terraform.tfvars gerados"

titulo "2/3  Terraform"
DIR_TF="$(caminho_para terraform "$STACK_REL")"
tf -chdir="$DIR_TF" init -input=false -reconfigure -backend-config=backend.hcl >/dev/null
ok "init"

ACAO=(apply)
if [ "$DESTRUIR" = "sim" ]; then
  ACAO=(destroy)
fi
if [ "$APROVAR_AUTO" = "sim" ]; then
  ACAO+=(-auto-approve)
fi
tf -chdir="$DIR_TF" "${ACAO[@]}" -input=false -var-file=terraform.tfvars

titulo "3/3  Pronto"
if [ "$DESTRUIR" = "sim" ]; then
  ok "Dashboards, alertas e monitores removidos do New Relic."
else
  tf -chdir="$DIR_TF" output dashboards
fi
