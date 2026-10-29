#!/usr/bin/env bash
# =============================================================================
# Publica a imagem da aplicação e faz o deploy num ambiente do cluster EKS.
#
#   AMBIENTE=homologacao ./infra/scripts/deploy.sh      (padrão)
#   AMBIENTE=producao    ./infra/scripts/deploy.sh
#
# É o equivalente manual do .github/workflows/cd-aws.yml.
#
# ─── O que precisa existir antes ─────────────────────────────────────────────
#
#   autopecas-infra-k8s     cluster e ECR           (state k8s/...)
#   autopecas-infra-db      banco do ambiente       (state db/<ambiente>/...)
#   autopecas-lambda-auth   emissor do JWT          (state lambda/<ambiente>/...)
#
# Os três guardam o state no mesmo bucket; este script só LÊ as saídas deles
# (ver output_remoto em lib.sh). E, uma vez por banco, o administrador cria o
# usuário `autopecas_app` com os scripts do repositório do banco — depois das
# migrações, porque as permissões são dadas tabela por tabela:
#
#   SOMENTE_MIGRACAO=sim ./infra/scripts/deploy.sh   # 1. cria as tabelas
#   (scripts criar-usuario-*.sql do repositório do banco)   # 2. permissões
#   PULAR_MIGRACAO=sim   ./infra/scripts/deploy.sh   # 3. sobe a aplicação
#
# ─── Senhas, por variável de ambiente ────────────────────────────────────────
#
#   APP_DB_PASSWORD     senha do `autopecas_app`, com que a aplicação roda
#   DB_ADMIN_USERNAME   dono do banco, usado SÓ pelo Job de migração
#   DB_ADMIN_PASSWORD
#   NEW_RELIC_LICENSE_KEY  opcional: liga o agente do New Relic (APM, traces,
#                          logs e eventos de negócio). Sem ela, a aplicação
#                          sobe igual, sem monitoramento.
#
# Viram Secrets do Kubernetes por entrada padrão (stdin), nunca por argumento
# de linha de comando — que apareceria na lista de processos e no histórico.
#
# O Docker do HOST constrói a imagem — não o container toolbox: construir
# imagem dentro de container exigiria Docker-in-Docker.
# =============================================================================

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

AMBIENTE="${AMBIENTE:-homologacao}"
PULAR_TESTES="${PULAR_TESTES:-nao}"
PULAR_MIGRACAO="${PULAR_MIGRACAO:-nao}"
# Para depois das migrações, sem subir a aplicação. É a PRIMEIRA vez de cada
# banco: as migrações criam as tabelas, o administrador dá ao `autopecas_app` e
# ao `lambda_auth` as permissões NELAS (scripts do repositório do banco), e só
# então um deploy completo sobe a aplicação — que antes disso falharia ao ler
# tabelas sem permissão.
SOMENTE_MIGRACAO="${SOMENTE_MIGRACAO:-nao}"
APP_DB_USERNAME="${APP_DB_USERNAME:-autopecas_app}"

# Cada ambiente: o NodePort por onde o NLB o alcança (mapa `node_ports` do
# repositório autopecas-infra-k8s) e a fatia da capacidade do cluster que o HPA
# pode usar (ver infra/k8s/aws/40-app-hpa.yaml).
case "$AMBIENTE" in
  producao)    NODE_PORT=30080; HPA_MIN=2; HPA_MAX=8 ;;
  homologacao) NODE_PORT=30081; HPA_MIN=1; HPA_MAX=3 ;;
  *) morrer "AMBIENTE deve ser 'homologacao' ou 'producao' (recebido: '${AMBIENTE}')." ;;
esac
NAMESPACE="$AMBIENTE"

if [ "$SOMENTE_MIGRACAO" = "sim" ] && [ "$PULAR_MIGRACAO" = "sim" ]; then
  morrer "SOMENTE_MIGRACAO=sim e PULAR_MIGRACAO=sim não fazem sentido juntas."
fi
if [ -z "${APP_DB_PASSWORD:-}" ]; then
  morrer "APP_DB_PASSWORD não definida — é a senha do usuário ${APP_DB_USERNAME} no banco de ${AMBIENTE}."
fi
if [ "$PULAR_MIGRACAO" != "sim" ] && { [ -z "${DB_ADMIN_USERNAME:-}" ] || [ -z "${DB_ADMIN_PASSWORD:-}" ]; }; then
  morrer "DB_ADMIN_USERNAME e DB_ADMIN_PASSWORD são necessárias para a migração (ou PULAR_MIGRACAO=sim)."
fi

exigir_docker
exigir_aws_configurada

REGIAO="$(regiao)"

# ─────────────────────────────────────────────────────────────────────────────
# 1. Descobrir o ambiente pelas saídas dos outros repositórios
#
# Nada fixado: a URL do ECR carrega o ID da conta, o endpoint do banco e o
# emissor do JWT mudam a cada vez que a infraestrutura é recriada.
# ─────────────────────────────────────────────────────────────────────────────
titulo "1/6  Lendo o ambiente ${AMBIENTE}"

ECR_URL="$(output_remoto k8s/bootstrap/terraform.tfstate ecr_repository_url)"
CLUSTER="$(output_remoto k8s/cluster/terraform.tfstate cluster_name)"
DB_URL="$(output_remoto "db/${AMBIENTE}/terraform.tfstate" db_url)"
JWT_ISSUER="$(output_remoto "lambda/${AMBIENTE}/terraform.tfstate" jwt_issuer)"
JWT_JWKS_URI="$(output_remoto "lambda/${AMBIENTE}/terraform.tfstate" jwks_url)"
API_URL="$(output_remoto "lambda/${AMBIENTE}/terraform.tfstate" api_url)"

FALTANDO=0
for par in "ECR_URL:autopecas-infra-k8s (bootstrap)" "CLUSTER:autopecas-infra-k8s (cluster)" \
           "DB_URL:autopecas-infra-db (${AMBIENTE})" "JWT_ISSUER:autopecas-lambda-auth (${AMBIENTE})"; do
  var="${par%%:*}"; origem="${par#*:}"
  if [ -z "${!var}" ]; then
    erro "${var} vazio — o repositório ${origem} foi aplicado nesta conta?"
    FALTANDO=1
  fi
done
[ "$FALTANDO" -eq 0 ] || morrer "Faltam saídas de outros repositórios (bucket: $(bucket_state))."

ok "ECR      ${ECR_URL}"
ok "Cluster  ${CLUSTER} — namespace ${NAMESPACE}, NodePort ${NODE_PORT}"
ok "Banco    ${DB_URL%%\?*}"
ok "Emissor  ${JWT_ISSUER}"

# ─────────────────────────────────────────────────────────────────────────────
# 2. Testes
#
# Pelo Maven Wrapper quando existe Java local; senão, só o build do Dockerfile,
# que compila mas não roda a suíte.
# ─────────────────────────────────────────────────────────────────────────────
titulo "2/6  Testes"

if [ "$PULAR_TESTES" = "sim" ]; then
  aviso "Testes pulados (PULAR_TESTES=sim)"
elif tem java; then
  (cd "$REPO_ROOT" && ./mvnw --batch-mode clean verify)
  ok "Testes passaram"
else
  aviso "Java não encontrado no host — os testes serão pulados."
  aviso "O Dockerfile compila a aplicação de todo jeito, mas sem rodar a suíte."
fi

# ─────────────────────────────────────────────────────────────────────────────
# 3. Imagem
#
# A tag é o SHA do commit, nunca `latest`: com `latest`, "qual código está
# rodando?" não tem resposta, e um rollback depende de lembrar qual imagem era
# a anterior.
# ─────────────────────────────────────────────────────────────────────────────
titulo "3/6  Construindo e publicando a imagem"

if git -C "$REPO_ROOT" rev-parse --short=12 HEAD >/dev/null 2>&1; then
  TAG="$(git -C "$REPO_ROOT" rev-parse --short=12 HEAD)"
else
  # Fora de um repositório git (download do .zip, por exemplo).
  TAG="manual-$(date +%Y%m%d%H%M%S)"
  aviso "Sem commit git; usando a tag ${TAG}"
fi

IMAGEM="${ECR_URL}:${TAG}"

docker build \
  --tag "$IMAGEM" \
  --label "org.opencontainers.image.revision=${TAG}" \
  "$REPO_ROOT"

# Push com credencial do ECR num config.json TEMPORÁRIO — sem `docker login`.
#
# No Windows, `docker login` grava no Gerenciador de Credenciais do sistema, que
# tem limite de tamanho — e o token do ECR passa dele. A falha é "error storing
# credentials ... The stub received bad data", sem menção a tamanho nem a ECR.
#
# Um config.json vazio NÃO resolve: sem nenhuma credencial registrada, o Docker
# escolhe sozinho o gerenciador do sistema como padrão. Com a entrada já escrita
# no arquivo, ele lê dali e não consulta o sistema.
#
# Fora do Windows o problema não existe, mas o arranjo também não atrapalha: o
# token (válido por 12h) vive só durante o deploy, e o diretório é apagado na
# saída, inclusive em caso de erro.
DOCKER_CONFIG_ECR="$(mktemp -d)"
trap 'rm -rf "$DOCKER_CONFIG_ECR"' EXIT

# O `tr` tira o \r que a saída vinda do container pode trazer, e as quebras de
# linha que o `base64` do GNU insere a cada 76 caracteres.
TOKEN_ECR="$(awscli ecr get-login-password --region "$REGIAO" | tr -d '\r\n')"
AUTH_ECR="$(printf 'AWS:%s' "$TOKEN_ECR" | base64 | tr -d '\r\n')"
printf '{"auths":{"%s":{"auth":"%s"}}}\n' "${ECR_URL%%/*}" "$AUTH_ECR" \
  > "${DOCKER_CONFIG_ECR}/config.json"
unset TOKEN_ECR AUTH_ECR

DOCKER_CONFIG="$DOCKER_CONFIG_ECR" docker push "$IMAGEM"
ok "Publicado: ${IMAGEM}"

# ─────────────────────────────────────────────────────────────────────────────
# 4. Namespace, configuração e segredos
# ─────────────────────────────────────────────────────────────────────────────
titulo "4/6  Configuração do ambiente ${AMBIENTE}"

awscli eks update-kubeconfig --name "$CLUSTER" --region "$REGIAO" >/dev/null
ok "kubeconfig atualizado"

# Cópia renderizada DENTRO do repositório (infra/k8s/.rendered, ignorado pelo
# git), e não em /tmp: quando o kubectl roda no container, só o repositório
# está montado. E nunca no arquivo versionado: a URL do ECR carrega o ID da
# conta, e apareceria no `git diff` de quem rodou.
RENDER_REL="infra/k8s/.rendered/${AMBIENTE}"
RENDER_ABS="${REPO_ROOT}/${RENDER_REL}"
rm -rf "$RENDER_ABS"
mkdir -p "$RENDER_ABS"
cp "${REPO_ROOT}/${K8S_SUBDIR}/"*.yaml "$RENDER_ABS/"

# `|` como delimitador porque a URL do ECR contém barras. O sufixo em `-i`
# mantém compatibilidade entre o sed do GNU e o do BSD/macOS.
sed -i.bak \
  -e "s|IMAGE_URI|${IMAGEM}|g" \
  -e "s|NAMESPACE|${NAMESPACE}|g" \
  -e "s|NODE_PORT|${NODE_PORT}|g" \
  -e "s|HPA_MIN|${HPA_MIN}|g" \
  -e "s|HPA_MAX|${HPA_MAX}|g" \
  "$RENDER_ABS"/*.yaml
rm -f "$RENDER_ABS"/*.bak

# Marcador esquecido vira erro de schema ou, pior, um pod em ImagePullBackOff.
# Só linhas que não são comentário contam.
if grep -hvE '^\s*#' "$RENDER_ABS"/*.yaml | grep -qE 'IMAGE_URI|NAMESPACE|NODE_PORT|HPA_MIN|HPA_MAX'; then
  morrer "Sobrou marcador sem substituir nos manifestos renderizados."
fi

kubectl_ apply -f "$(caminho_para kubectl "${RENDER_REL}/00-namespace.yaml")"

# Recursos gerados aqui, e não versionados: os valores vêm das saídas dos
# outros repositórios e das variáveis de senha. `apply` por stdin torna o passo
# idempotente — reaplicar com o mesmo valor não muda nada.
b64() { printf '%s' "$1" | base64 | tr -d '\r\n'; }

kubectl_ apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: autopecas-config
  namespace: ${NAMESPACE}
  labels:
    app.kubernetes.io/part-of: autopecas
data:
  SPRING_PROFILES_ACTIVE: "prod"
  TZ: "UTC"
  DB_URL: "${DB_URL}"
  JWT_ISSUER: "${JWT_ISSUER}"
  JWT_JWKS_URI: "${JWT_JWKS_URI}"
  JWT_AUDIENCE: "autopecas-api"
  # As migrações são do Job, com o dono do banco; o usuário da aplicação não
  # tem DDL para executá-las.
  SPRING_FLYWAY_ENABLED: "false"
  # Observabilidade: o ambiente vai nos logs (ECS) e nos eventos de negócio, e
  # cada ambiente é uma aplicação separada no New Relic.
  AMBIENTE: "${AMBIENTE}"
  NEW_RELIC_APP_NAME: "autopecas-api-${AMBIENTE}"
  NEW_RELIC_LABELS: "ambiente:${AMBIENTE}"
---
apiVersion: v1
kind: Secret
metadata:
  name: autopecas-secrets
  namespace: ${NAMESPACE}
  labels:
    app.kubernetes.io/part-of: autopecas
type: Opaque
data:
  DB_USERNAME: $(b64 "$APP_DB_USERNAME")
  DB_PASSWORD: $(b64 "$APP_DB_PASSWORD")
$( [ -n "${NEW_RELIC_LICENSE_KEY:-}" ] && printf '  NEW_RELIC_LICENSE_KEY: %s\n' "$(b64 "$NEW_RELIC_LICENSE_KEY")" )
EOF
ok "ConfigMap e Secret da aplicação aplicados"
if [ -n "${NEW_RELIC_LICENSE_KEY:-}" ]; then
  ok "New Relic ligado — aplicação autopecas-api-${AMBIENTE}"
else
  aviso "NEW_RELIC_LICENSE_KEY não definida: a aplicação sobe sem o agente do New Relic."
fi

# ─────────────────────────────────────────────────────────────────────────────
# 5. Migração, com o dono do banco
# ─────────────────────────────────────────────────────────────────────────────
titulo "5/6  Migração do banco"

if [ "$PULAR_MIGRACAO" = "sim" ]; then
  aviso "Migração pulada (PULAR_MIGRACAO=sim)"
else
  kubectl_ apply -f - <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: autopecas-migracao
  namespace: ${NAMESPACE}
  labels:
    app.kubernetes.io/part-of: autopecas
type: Opaque
data:
  FLYWAY_USER: $(b64 "$DB_ADMIN_USERNAME")
  FLYWAY_PASSWORD: $(b64 "$DB_ADMIN_PASSWORD")
EOF

  # Job é imutável: o da execução anterior precisa sair antes de o novo entrar.
  kubectl_ delete job autopecas-migracao -n "$NAMESPACE" --ignore-not-found --wait=true >/dev/null
  kubectl_ apply -f "$(caminho_para kubectl "${RENDER_REL}/20-migracao-job.yaml")"

  if ! kubectl_ wait --for=condition=complete job/autopecas-migracao -n "$NAMESPACE" --timeout=10m; then
    erro "A migração falhou ou não terminou. Log do Flyway:"
    kubectl_ logs job/autopecas-migracao -n "$NAMESPACE" -c flyway --tail=60 || true
    morrer "Deploy interrompido ANTES de trocar a aplicação: a versão anterior continua no ar."
  fi
  ok "Migração aplicada"
  kubectl_ logs job/autopecas-migracao -n "$NAMESPACE" -c flyway --tail=5 2>/dev/null | sed 's/^/    /' || true

  # A credencial do dono do banco não precisa ficar no cluster entre deploys.
  kubectl_ delete secret autopecas-migracao -n "$NAMESPACE" --ignore-not-found >/dev/null
fi

if [ "$SOMENTE_MIGRACAO" = "sim" ]; then
  titulo "Migração concluída (SOMENTE_MIGRACAO=sim)"
  cat <<EOF

  As tabelas existem. Antes de subir a aplicação, o administrador do banco
  precisa dar as permissões ao autopecas_app e ao lambda_auth — scripts
  criar-usuario-*.sql do repositório autopecas-infra-db. Depois:

      AMBIENTE=${AMBIENTE} ./infra/scripts/deploy.sh

EOF
  exit 0
fi

# ─────────────────────────────────────────────────────────────────────────────
# 6. Aplicação
# ─────────────────────────────────────────────────────────────────────────────
titulo "6/6  Aplicando a aplicação"

kubectl_ apply \
  -f "$(caminho_para kubectl "${RENDER_REL}/30-app-deployment.yaml")" \
  -f "$(caminho_para kubectl "${RENDER_REL}/40-app-hpa.yaml")"

if ! kubectl_ rollout status deployment/autopecas-api -n "$NAMESPACE" --timeout=10m; then
  erro "O rollout da API falhou. Diagnóstico:"
  kubectl_ get pods -n "$NAMESPACE" -o wide || true
  kubectl_ logs deployment/autopecas-api -n "$NAMESPACE" --tail=100 || true
  exit 1
fi

titulo "Deploy concluído"
cat <<EOF

  Ambiente   ${AMBIENTE}
  Imagem     ${IMAGEM}
  API        ${API_URL:-<saída api_url do repositório da Lambda>}

  Verificar ponta a ponta:

      AMBIENTE=${AMBIENTE} SMOKE_DOCUMENTO=<CPF de um cliente ativo> ./infra/scripts/smoke.sh

EOF
