#!/usr/bin/env bash
# =============================================================================
# Publica a imagem da aplicação e faz o deploy no cluster.
#
#   ./infra/scripts/deploy.sh
#
# É o equivalente manual do .github/workflows/cd-aws.yml, para quem não tem os
# secrets do repositório configurados — o caso de quem clonou para avaliar.
#
# Pré-requisitos: ./infra/scripts/up.sh já executado com sucesso.
#
# Diferente do up.sh, aqui o Docker do HOST é usado para construir a imagem —
# não o container toolbox. Construir imagem dentro de container exigiria
# Docker-in-Docker, e o Docker já é pré-requisito de todo jeito.
# =============================================================================

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

PULAR_TESTES="${PULAR_TESTES:-nao}"

exigir_docker
exigir_aws_configurada

REGIAO="$(regiao)"

# ─────────────────────────────────────────────────────────────────────────────
# 1. Descobrir o ambiente pelos outputs do Terraform
#
# Nada hardcoded: a URL do ECR carrega o ID da conta, o nome do cluster vem de
# project_name. É o que permite este script rodar em qualquer conta AWS.
# ─────────────────────────────────────────────────────────────────────────────
titulo "1/6  Lendo o ambiente provisionado"

ECR_URL="$(tf_output _backend ecr_repository_url)"
CLUSTER="$(tf_output 03-cluster cluster_name)"
API_URL="$(tf_output 04-gateway-auth api_base_url)"

if [ -z "$ECR_URL" ] || [ -z "$CLUSTER" ]; then
  erro "Não encontrei os outputs do Terraform."
  echo "  Rode primeiro:  ./infra/scripts/up.sh" >&2
  exit 1
fi

ok "ECR      ${ECR_URL}"
ok "Cluster  ${CLUSTER}"

# ─────────────────────────────────────────────────────────────────────────────
# 2. Build do JAR
#
# Pelo Maven Wrapper quando existe Java local; senão, dentro do próprio
# multi-stage do Dockerfile, que já compila. Checar antes evita baixar uma
# imagem de JDK sem necessidade.
# ─────────────────────────────────────────────────────────────────────────────
titulo "2/6  Testes e build"

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
# 4. Kubeconfig
# ─────────────────────────────────────────────────────────────────────────────
titulo "4/6  Apontando o kubectl para o cluster"
awscli eks update-kubeconfig --name "$CLUSTER" --region "$REGIAO" >/dev/null
ok "kubeconfig atualizado"

# A configuração da aplicação vem da stack 04. Aplicar os manifestos sem ela
# produz um pod em CreateContainerConfigError, cuja mensagem não deixa óbvio
# que falta rodar o Terraform.
titulo "5/6  Conferindo a configuração no cluster"
FALTANDO=0
for recurso in configmap/autopecas-config secret/autopecas-secrets \
               configmap/keycloak-config secret/keycloak-secrets \
               configmap/keycloak-realm; do
  if ! kubectl_ get "$recurso" -n autopecas >/dev/null 2>&1; then
    erro "$recurso não existe no namespace autopecas"
    FALTANDO=1
  fi
done
if [ "$FALTANDO" -eq 1 ]; then
  morrer "Rode a stack 04-gateway-auth (./infra/scripts/up.sh) antes do deploy."
fi
ok "ConfigMaps e Secrets presentes"

# ─────────────────────────────────────────────────────────────────────────────
# 6. Manifestos
#
# A substituição de `IMAGE_URI` acontece numa CÓPIA: editar o arquivo versionado
# deixaria a URL do ECR de uma conta específica no `git diff` de quem rodou o
# script.
#
# A cópia vai para dentro do repositório, em infra/k8s/.rendered (ignorado pelo
# git), e não para /tmp: quando o kubectl roda no container, só o repositório
# está montado — um diretório temporário do host não existe para ele.
# ─────────────────────────────────────────────────────────────────────────────
titulo "6/6  Aplicando os manifestos"

RENDER_REL="infra/k8s/.rendered"
RENDER_ABS="${REPO_ROOT}/${RENDER_REL}"

rm -rf "$RENDER_ABS"
mkdir -p "$RENDER_ABS"
cp "${REPO_ROOT}/${K8S_SUBDIR}/"*.yaml "$RENDER_ABS/"

# `|` como delimitador porque a URL do ECR contém barras. O sufixo em `-i`
# mantém compatibilidade entre o sed do GNU e o do BSD/macOS, que divergem
# justamente no `-i` sem argumento.
sed -i.bak "s|IMAGE_URI|${IMAGEM}|g" "${RENDER_ABS}/30-app-deployment.yaml"
rm -f "${RENDER_ABS}"/*.bak

if ! grep -q "$IMAGEM" "${RENDER_ABS}/30-app-deployment.yaml"; then
  morrer "A substituição de IMAGE_URI falhou — o manifesto seguiria com o marcador."
fi

kubectl_ apply -f "$(caminho_para kubectl "$RENDER_REL")"

titulo "Aguardando o rollout"
# Keycloak primeiro: é o mais lento do stack (augmentation do Quarkus + import
# do realm). Esperar a API antes consumiria a janela de timeout dele.
echo "    Keycloak (pode levar até 15 min no primeiro boot)..."
kubectl_ rollout status deployment/keycloak -n autopecas --timeout=15m

echo "    API..."
if ! kubectl_ rollout status deployment/autopecas-api -n autopecas --timeout=10m; then
  erro "O rollout da API falhou. Diagnóstico:"
  kubectl_ get pods -n autopecas -o wide || true
  kubectl_ logs deployment/autopecas-api -n autopecas --tail=100 || true
  exit 1
fi

titulo "Deploy concluído"
cat <<EOF

  Imagem   ${IMAGEM}
  API      ${API_URL}

  Verificar ponta a ponta:

      ./infra/scripts/smoke.sh

EOF
