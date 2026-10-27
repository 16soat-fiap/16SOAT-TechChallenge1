#!/usr/bin/env bash
# =============================================================================
# Provisiona a infraestrutura completa na AWS, do zero.
#
#   ./infra/scripts/up.sh
#
# Único pré-requisito na máquina: Docker, e credencial da AWS configurada.
# Terraform, AWS CLI e kubectl vêm do container toolbox se não existirem
# localmente.
#
# O script é IDEMPOTENTE: rodar de novo reconcilia o que já existe em vez de
# duplicar. Interrompido no meio, basta rodar outra vez.
#
# O que ele faz, e por que nesta ordem:
#
#   1. _backend  → cria o bucket de state e o ECR. State LOCAL, porque é ele
#                  que cria o bucket onde os outros guardam o state.
#   2. escreve backend.hcl e terraform.tfvars a partir dos outputs do passo 1.
#                  Estes dois arquivos NÃO são versionados: contêm o nome de um
#                  bucket específico da conta de quem rodou. É exatamente a
#                  parte que não pode vir no repositório — e é por isso que o
#                  script a gera, em vez de pedir que seja preenchida à mão.
#   3. 01 → 02 → 03 → 04, em ordem. Cada stack lê os outputs da anterior pelo
#                  state no S3, então a ordem não é estética.
#
# Tempo: ~25 a 35 minutos no total, quase tudo na stack 03 (o control plane do
# EKS leva ~12 min e o node group outros ~5).
#
# Custo: ~US$ 183/mês com tudo ligado. Ver a tabela em
# seção "Custo" do README da raiz e o down.sh para derrubar.
# =============================================================================

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

APROVAR_AUTO="${APROVAR_AUTO:-nao}"
APLICAR=(apply)
if [ "$APROVAR_AUTO" = "sim" ]; then
  APLICAR+=(-auto-approve)
fi

# ─────────────────────────────────────────────────────────────────────────────
# 0. Pré-requisitos
# ─────────────────────────────────────────────────────────────────────────────
titulo "Pré-requisitos"

if _no_container terraform || _no_container aws; then
  exigir_docker
  garantir_toolbox
  ok "Ferramentas vindas do container $TOOLBOX_IMAGE"
else
  ok "Terraform e AWS CLI encontrados localmente"
fi

exigir_aws_configurada

REGIAO="$(regiao)"
ok "Região: ${REGIAO}"

# ─────────────────────────────────────────────────────────────────────────────
# Cota de vCPU — o bloqueio mais provável numa conta AWS nova.
#
# Conta recém-criada costuma vir com 5 vCPUs de instâncias On-Demand Standard.
# Dois nós t3.medium são 4 vCPUs e cabem; o Cluster Autoscaler subindo para 3
# nós pede 6 e é RECUSADO. O sintoma é um node group que fica em CREATE_FAILED
# ou um scale-up que nunca acontece, com a mensagem de cota enterrada no evento
# do Auto Scaling Group — longe de onde se procura.
#
# Só avisa, nunca bloqueia: a cota pode já ter sido aumentada, e a leitura
# depende de uma permissão (service-quotas) que a credencial pode não ter.
# ─────────────────────────────────────────────────────────────────────────────
conferir_cota_vcpu() {
  local tipo="${NODE_INSTANCE_TYPE:-t3.medium}"
  local max_nos="${NODE_MAX_SIZE:-4}"

  local cota vcpus_por_no necessario
  # L-1216C47A = "Running On-Demand Standard (A, C, D, H, I, M, R, T, Z)
  # instances", medida em vCPUs.
  cota="$(awscli service-quotas get-service-quota \
            --service-code ec2 --quota-code L-1216C47A \
            --region "$REGIAO" \
            --query 'Quota.Value' --output text 2>/dev/null || true)"

  if [ -z "$cota" ] || [ "$cota" = "None" ]; then
    aviso "Não foi possível ler a cota de vCPU (falta permissão em service-quotas?)."
    aviso "Se o node group falhar com erro de cota, peça aumento do limite"
    aviso "'Running On-Demand Standard instances' para pelo menos 8 vCPUs."
    return 0
  fi

  vcpus_por_no="$(awscli ec2 describe-instance-types \
                    --instance-types "$tipo" --region "$REGIAO" \
                    --query 'InstanceTypes[0].VCpuInfo.DefaultVCpus' \
                    --output text 2>/dev/null || echo 2)"

  necessario=$(( vcpus_por_no * max_nos ))
  # A cota vem como float ("5.0"); corta a parte decimal para comparar.
  local cota_int="${cota%%.*}"

  if [ "$cota_int" -ge "$necessario" ]; then
    ok "Cota de vCPU: ${cota_int} (precisa de ${necessario} para ${max_nos}× ${tipo})"
  else
    aviso "Cota de vCPU é ${cota_int}, mas o autoscaling pode pedir ${necessario}"
    aviso "(${max_nos} nós × ${vcpus_por_no} vCPU do ${tipo})."
    echo "    O ambiente SOBE — 2 nós usam $(( vcpus_por_no * 2 )) vCPUs —, mas o scale-up"
    echo "    além disso será recusado, e é justamente o que demonstra o autoscaling."
    echo
    echo "    Para pedir aumento (gratuito, costuma sair em minutos):"
    echo "      https://${REGIAO}.console.aws.amazon.com/servicequotas/home/services/ec2/quotas/L-1216C47A"
    echo
  fi
}

titulo "Conferindo cota de vCPU"
conferir_cota_vcpu

# ─────────────────────────────────────────────────────────────────────────────
# 1. Bootstrap — bucket de state, ECR e (opcionalmente) o papel do GitHub
# ─────────────────────────────────────────────────────────────────────────────
titulo "1/6  _backend — bucket de state e ECR"

VARS_BACKEND=(-var "aws_region=${REGIAO}")

# O papel OIDC do GitHub só é criado se o repositório for informado. Quem está
# só provisionando para avaliar não precisa dele — ver a explicação em
# _backend/github-oidc.tf.
if [ -n "${GITHUB_REPOSITORY:-}" ]; then
  VARS_BACKEND+=(-var "enable_github_oidc=true" -var "github_repository=${GITHUB_REPOSITORY}")
  aviso "OIDC do GitHub habilitado para ${GITHUB_REPOSITORY}"
else
  echo "    OIDC do GitHub desabilitado. Para habilitar:"
  echo "      GITHUB_REPOSITORY=16soat-fiap/16SOAT-TechChallenge1 ./infra/scripts/up.sh"
fi

tf_stack _backend init -input=false
tf_stack _backend "${APLICAR[@]}" -input=false "${VARS_BACKEND[@]}"

STATE_BUCKET="$(tf_output _backend state_bucket)"
ECR_URL="$(tf_output _backend ecr_repository_url)"
GH_ROLE="$(tf_output _backend github_deploy_role_arn)"

if [ -z "$STATE_BUCKET" ]; then
  morrer "A stack _backend não devolveu state_bucket. Confira o erro acima."
fi
ok "Bucket de state: ${STATE_BUCKET}"
ok "ECR: ${ECR_URL}"

# ─────────────────────────────────────────────────────────────────────────────
# 2. Configuração local, derivada do passo anterior
#
# Reescrito a cada execução de propósito: são arquivos DERIVADOS, e um
# backend.hcl apontando para um bucket de outra conta é a causa mais provável
# de um "NoSuchBucket" no init.
# ─────────────────────────────────────────────────────────────────────────────
titulo "2/6  Configuração local (backend.hcl e terraform.tfvars)"

cat > "${REPO_ROOT}/${TF_SUBDIR}/backend.hcl" <<EOF
# GERADO por infra/scripts/up.sh — não versionado, não edite à mão.
bucket = "${STATE_BUCKET}"
region = "${REGIAO}"
EOF
ok "backend.hcl escrito"

ADMINS_TF='cluster_admin_role_arns = []'
if [ -n "$GH_ROLE" ]; then
  ADMINS_TF="cluster_admin_role_arns = [\"${GH_ROLE}\"]"
fi

cat > "${REPO_ROOT}/${TF_SUBDIR}/terraform.tfvars" <<EOF
# GERADO por infra/scripts/up.sh — não versionado, não edite à mão.
#
# Para mudar dimensionamento ou região, exporte a variável antes de rodar o
# script, ou edite e rode as stacks manualmente (ver o README da raiz).
state_bucket = "${STATE_BUCKET}"
aws_region   = "${REGIAO}"

# Papéis IAM com acesso de admin ao cluster, além de quem rodou o apply.
# Sem o papel do GitHub aqui, o kubectl do pipeline autentica e não autoriza.
${ADMINS_TF}
EOF
ok "terraform.tfvars escrito"

# ─────────────────────────────────────────────────────────────────────────────
# 3. As quatro stacks, em ordem
# ─────────────────────────────────────────────────────────────────────────────
PASSO=3
for stack in "${STACKS[@]}"; do
  titulo "${PASSO}/6  ${stack}"
  PASSO=$((PASSO + 1))

  # -reconfigure, e não -migrate-state: o backend.hcl pode ter mudado entre
  # execuções (outra conta, outra região), e aí o Terraform perguntaria se deve
  # migrar o state — uma pergunta interativa num script.
  tf_stack "$stack" init -input=false -reconfigure \
    -backend-config="$(caminho_backend_hcl)"

  tf_stack "$stack" "${APLICAR[@]}" -input=false \
    -var-file="$(caminho_tfvars)"

  if [ "$stack" = "03-cluster" ]; then
    CLUSTER="$(tf_output 03-cluster cluster_name)"
    ok "Cluster ${CLUSTER} de pé"
  fi
done

# ─────────────────────────────────────────────────────────────────────────────
# 4. Kubeconfig
# ─────────────────────────────────────────────────────────────────────────────
titulo "Configurando o kubeconfig"
CLUSTER="$(tf_output 03-cluster cluster_name)"
awscli eks update-kubeconfig --name "$CLUSTER" --region "$REGIAO" >/dev/null
ok "kubectl apontado para ${CLUSTER}"

# ─────────────────────────────────────────────────────────────────────────────
# 5. Resumo
# ─────────────────────────────────────────────────────────────────────────────
API_URL="$(tf_output 04-gateway-auth api_base_url)"

titulo "Infraestrutura pronta"
cat <<EOF

  URL da API      ${API_URL}
  Health check    ${API_URL}/health
  Swagger         ${API_URL}/swagger-ui.html
  Cluster         ${CLUSTER}
  ECR             ${ECR_URL}

  A APLICAÇÃO AINDA NÃO ESTÁ NO AR — falta publicar a imagem:

      ./infra/scripts/deploy.sh

  Depois, para verificar ponta a ponta:

      ./infra/scripts/smoke.sh

  Para derrubar e parar de pagar:

      ./infra/scripts/down.sh

EOF
