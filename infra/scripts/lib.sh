#!/usr/bin/env bash
# =============================================================================
# Funções compartilhadas pelos scripts de infraestrutura.
#
# Não executa nada por conta própria — é feito para ser carregado com `source`.
#
# A ideia central: a máquina de quem roda precisa APENAS de Docker. Terraform,
# AWS CLI e kubectl vêm da imagem toolbox (infra/docker/toolbox.Dockerfile).
# Mas se os binários já existirem localmente, eles são usados — é mais rápido e
# evita a camada de container.
#
# Nenhuma função aqui depende de `jq`, `curl` ou qualquer utilitário no host: a
# máquina que vai avaliar o projeto pode ser Windows, e assumir um ambiente
# POSIX completo é o erro clássico deste tipo de script.
# =============================================================================

# Interpretado por bash, não por sh.
#
# Os scripts usam arrays (`args=(...)`), `[[ ]]`, `${!var}` e `pipefail` — nada
# disso existe no `sh` do Debian/Ubuntu (dash). Executados como `sh up.sh`, o
# erro seria um "Syntax error: ( unexpected" no meio da execução, sem relação
# óbvia com a causa. Melhor dizer o que fazer.
if [ -z "${BASH_VERSION:-}" ]; then
  echo "Estes scripts precisam de bash, não de sh." >&2
  echo "  Rode:  bash infra/scripts/<script>.sh   (ou ./infra/scripts/<script>.sh)" >&2
  echo "  No Windows, use Git Bash ou WSL — PowerShell e cmd não servem." >&2
  exit 1
fi

set -euo pipefail

# ─────────────────────────────────────────────────────────────────────────────
# Caminhos
# ─────────────────────────────────────────────────────────────────────────────
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TF_SUBDIR="infra/terraform/aws"
K8S_SUBDIR="infra/k8s/aws"

STACKS=(01-network 02-database 03-cluster 04-gateway-auth)

TOOLBOX_IMAGE="${TOOLBOX_IMAGE:-autopecas-toolbox:local}"

# Cache de providers do Terraform FORA do repositório. Sem isto, cada stack
# baixa ~600 MB do provider da AWS por conta própria — e, numa pasta
# sincronizada (OneDrive, Dropbox), esses binários entram na sincronização.
TF_CACHE="${TF_CACHE:-${HOME}/.terraform-plugin-cache}"

# "auto" usa o binário local quando existe; "sempre" força o container, o que
# é útil para reproduzir exatamente o ambiente do CI.
USAR_CONTAINER="${USAR_CONTAINER:-auto}"

# ─────────────────────────────────────────────────────────────────────────────
# Saída
# ─────────────────────────────────────────────────────────────────────────────
if [ -t 1 ]; then
  _AZUL=$'\033[1;34m'; _VERDE=$'\033[1;32m'; _AMARELO=$'\033[1;33m'
  _VERMELHO=$'\033[1;31m'; _FIM=$'\033[0m'
else
  _AZUL=''; _VERDE=''; _AMARELO=''; _VERMELHO=''; _FIM=''
fi

titulo() { printf '\n%s━━━ %s%s\n' "$_AZUL" "$*" "$_FIM"; }
ok()     { printf '%s✓%s %s\n' "$_VERDE" "$_FIM" "$*"; }
aviso()  { printf '%s!%s %s\n' "$_AMARELO" "$_FIM" "$*"; }
erro()   { printf '%s✗%s %s\n' "$_VERMELHO" "$_FIM" "$*" >&2; }
morrer() { erro "$*"; exit 1; }

# ─────────────────────────────────────────────────────────────────────────────
# Pré-requisitos
# ─────────────────────────────────────────────────────────────────────────────
tem() { command -v "$1" >/dev/null 2>&1; }

exigir_docker() {
  if ! tem docker; then
    morrer "Docker não encontrado. Instale o Docker e rode de novo."
  fi
  if ! docker version --format '{{.Server.Version}}' >/dev/null 2>&1; then
    morrer "O Docker está instalado mas o engine não responde. Inicie o Docker Desktop."
  fi
}

# Constrói a imagem toolbox se ela ainda não existir. Só é chamada quando algum
# binário falta — numa máquina que já tem terraform, aws e kubectl, a imagem
# nunca é construída.
garantir_toolbox() {
  if docker image inspect "$TOOLBOX_IMAGE" >/dev/null 2>&1; then
    return 0
  fi
  exigir_docker
  titulo "Construindo a imagem toolbox (uma vez só, ~1 min)"
  docker build \
    -f "${REPO_ROOT}/infra/docker/toolbox.Dockerfile" \
    -t "$TOOLBOX_IMAGE" \
    "${REPO_ROOT}" >/dev/null
  ok "Imagem $TOOLBOX_IMAGE pronta"
}

# ─────────────────────────────────────────────────────────────────────────────
# Execução de ferramentas
#
# `ferramenta <nome> <args...>` roda localmente se o binário existir, senão no
# container.
#
# O container monta:
#   • o repositório em /repo       — o código e o state local do _backend
#   • ~/.aws  em /root/.aws (ro)   — credenciais, nunca copiadas
#   • ~/.kube em /root/.kube       — kubeconfig; GRAVÁVEL, porque
#                                    `aws eks update-kubeconfig` escreve nele
#   • o cache de providers         — fora do repositório, ver TF_CACHE
# ─────────────────────────────────────────────────────────────────────────────
_rodar_no_container() {
  garantir_toolbox
  mkdir -p "${TF_CACHE}" "${HOME}/.kube"

  local args=(
    run --rm -i
    -v "${REPO_ROOT}:/repo"
    -v "${TF_CACHE}:/cache"
    -v "${HOME}/.kube:/root/.kube"
    -e TF_PLUGIN_CACHE_DIR=/cache
    -e TF_IN_AUTOMATION=1
    -w /repo
  )

  # Credenciais: o diretório ~/.aws quando existe, e sempre as variáveis de
  # ambiente — é como chegam credenciais temporárias (AWS SSO, STS, laboratório
  # de curso), que não ficam em arquivo.
  #
  # Cada teste em `if` explícito, e não `[ ... ] && args+=(...)`: sob
  # `set -e`, um `&&` cuja condição é falsa devolve 1 e aborta o script.
  if [ -d "${HOME}/.aws" ]; then
    args+=(-v "${HOME}/.aws:/root/.aws:ro")
  fi

  # Exceção à montagem só-leitura: o cache do `aws login`. A credencial dele
  # dura 15 minutos e o CLI a renova reescrevendo este diretório — sem escrita,
  # um apply longo perde a autenticação no meio. Ver toolbox-entrypoint.sh.
  if [ -d "${HOME}/.aws/login" ]; then
    args+=(-v "${HOME}/.aws/login:/root/.aws/login")
  fi

  local nome_var
  for nome_var in AWS_PROFILE AWS_REGION AWS_DEFAULT_REGION \
                  AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN; do
    if [ -n "${!nome_var:-}" ]; then
      args+=(-e "$nome_var")
    fi
  done

  if [ -t 0 ]; then
    args+=(-t)
  fi

  # MSYS_NO_PATHCONV impede o Git Bash do Windows de converter "/repo" num
  # caminho do Windows antes de o docker ver o argumento.
  MSYS_NO_PATHCONV=1 docker "${args[@]}" "$TOOLBOX_IMAGE" "$@"
}

# Decide se a ferramenta roda no container. Retorna 0 para "sim".
_no_container() {
  if [ "$USAR_CONTAINER" = "sempre" ]; then
    return 0
  fi

  # O kubectl é um caso especial: o kubeconfig escrito por
  # `aws eks update-kubeconfig` autentica com um exec plugin que INVOCA o
  # AWS CLI a cada chamada —
  #
  #     users[].user.exec: { command: aws, args: [eks, get-token, ...] }
  #
  # Então um kubectl local com um `aws` ausente falha com
  # "exec: aws: executable file not found", mesmo com o kubeconfig correto.
  # É o modo misto que aparece de verdade: kubectl vem junto com o Docker
  # Desktop, o AWS CLI não.
  #
  # Mandar os dois para o container mantém a dupla coerente. E como
  # `caminho_para` consulta esta mesma função, os caminhos passados ao kubectl
  # acompanham a decisão automaticamente.
  if [ "$1" = "kubectl" ] && ! tem aws; then
    return 0
  fi

  if tem "$1"; then
    return 1
  fi
  return 0
}

ferramenta() {
  local nome="$1"; shift
  if _no_container "$nome"; then
    _rodar_no_container "$nome" "$@"
  else
    TF_PLUGIN_CACHE_DIR="${TF_CACHE}" "$nome" "$@"
  fi
}

tf()       { ferramenta terraform "$@"; }
awscli()   { ferramenta aws "$@"; }
kubectl_() { ferramenta kubectl "$@"; }

# ─────────────────────────────────────────────────────────────────────────────
# Terraform por stack
#
# `-chdir` precisa de um caminho válido no contexto onde o Terraform roda: o
# container vê /repo, o binário local vê o caminho do host.
# ─────────────────────────────────────────────────────────────────────────────
_raiz_terraform() {
  if _no_container terraform; then
    echo "/repo"
  else
    echo "$REPO_ROOT"
  fi
}

tf_stack() {
  local stack="$1"; shift
  tf -chdir="$(_raiz_terraform)/${TF_SUBDIR}/${stack}" "$@"
}

# Caminhos ABSOLUTOS para os arquivos de configuração compartilhados.
#
# Nunca "../backend.hcl": combinado com `-chdir`, a resolução de caminho
# relativo em `-backend-config` e `-var-file` depende de detalhe de versão do
# Terraform — e o sintoma de errar é um "NoSuchBucket" ou um apply pedindo
# variáveis interativamente, nenhum dos dois apontando para a causa.
caminho_backend_hcl() { echo "$(_raiz_terraform)/${TF_SUBDIR}/backend.hcl"; }
caminho_tfvars()      { echo "$(_raiz_terraform)/${TF_SUBDIR}/terraform.tfvars"; }

# Traduz um caminho relativo ao repositório para o que a ferramenta vai ver.
#
#   caminho_para kubectl infra/k8s/aws   → /repo/infra/k8s/aws   (container)
#                                        → <REPO_ROOT>/infra/k8s/aws  (local)
#
# Necessário porque o container só monta o repositório: um arquivo em /tmp do
# host não existe para ele. É por isso que os manifestos renderizados são
# escritos DENTRO do repositório (num diretório ignorado pelo git) em vez de
# num diretório temporário do sistema.
caminho_para() {
  local ferramenta_nome="$1" relativo="$2"
  if _no_container "$ferramenta_nome"; then
    echo "/repo/${relativo}"
  else
    echo "${REPO_ROOT}/${relativo}"
  fi
}

# Lê um output, devolvendo vazio quando ele não existe ou é nulo.
#
# `output -raw` falha em output nulo — que é exatamente o caso de
# `github_deploy_role_arn` com o OIDC desabilitado. O `|| true` transforma
# isso em string vazia, sem precisar de `jq` no host.
tf_output() {
  local stack="$1" nome="$2"
  tf_stack "$stack" output -raw "$nome" 2>/dev/null || true
}

# ─────────────────────────────────────────────────────────────────────────────
# AWS
# ─────────────────────────────────────────────────────────────────────────────

# `--query` + `--output text` em vez de `jq`: o parsing acontece dentro do AWS
# CLI, que já é uma dependência, em vez de exigir mais um utilitário no host.
exigir_aws_configurada() {
  titulo "Conferindo credenciais da AWS"
  local identidade
  if ! identidade="$(awscli sts get-caller-identity \
                        --query 'join(`  |  `, [Account, Arn])' \
                        --output text 2>&1)"; then
    erro "Não foi possível autenticar na AWS."
    cat >&2 <<'AJUDA'

  Configure de UMA destas formas:

    aws login                          # login do console, sem chave (CLI >= 2.32)
    aws configure                      # chave de acesso de um usuário IAM
    export AWS_PROFILE=meu-perfil      # perfil já existente em ~/.aws/config
    export AWS_ACCESS_KEY_ID=...       # credencial temporária (SSO, STS, lab)
           AWS_SECRET_ACCESS_KEY=...
           AWS_SESSION_TOKEN=...

  Sem o AWS CLI instalado, exporte as variáveis — os scripts as repassam para
  o container.

AJUDA
    exit 1
  fi
  ok "Autenticado: ${identidade}"
}

regiao() {
  echo "${AWS_REGION:-${AWS_DEFAULT_REGION:-us-east-1}}"
}
