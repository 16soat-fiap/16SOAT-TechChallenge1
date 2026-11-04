#!/usr/bin/env bash
# =============================================================================
# Imprime a configuração dos repositórios do banco e da Lambda para um
# ambiente, com os valores tirados das saídas do autopecas-infra-k8s (e, para a
# Lambda, do banco). Só lê o bucket de state: não aplica nada.
#
#   AMBIENTE=homologacao ./infra/scripts/valores-aws.sh <parte>
#
#   banco           → autopecas-infra-db/terraform/environments/<amb>.tfvars
#   banco-backend   → autopecas-infra-db/terraform/environments/<amb>.s3.tfbackend
#   lambda          → autopecas-lambda-auth/terraform/environments/<amb>.tfvars
#   lambda-backend  → autopecas-lambda-auth/terraform/environments/<amb>.s3.tfbackend
#
# Exemplo, com os repositórios clonados lado a lado:
#
#   ./infra/scripts/valores-aws.sh banco > ../autopecas-infra-db/terraform/environments/homologacao.tfvars
#
# A ordem importa: `lambda` precisa do banco já aplicado, porque o security
# group da Lambda é criado pelo repositório do banco.
#
# Os valores mudam a cada vez que a infraestrutura é recriada (IDs de VPC,
# subnets, VPC Link) — gerar de novo a cada subida, não reaproveitar.
# =============================================================================

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

AMBIENTE="${AMBIENTE:-homologacao}"
PARTE="${1:-}"
case "$AMBIENTE" in
  homologacao|producao) ;;
  *) morrer "AMBIENTE deve ser 'homologacao' ou 'producao' (recebido: '${AMBIENTE}')." ;;
esac

# A saída padrão é o arquivo de configuração; avisos vão para a saída de erro.
exigir() { # <valor> <descrição>
  if [ -z "$1" ]; then
    erro "$2"
    exit 1
  fi
}

# Na primeira vez, a toolbox é construída aqui, com a mensagem na saída de erro
# — e não no meio do arquivo redirecionado.
if _no_container aws; then
  garantir_toolbox >&2
fi

BUCKET="$(bucket_state | tr -d '\r')"
exigir "$BUCKET" "Sem credencial da AWS (ver a ajuda de exigir_aws_configurada em lib.sh)."

case "$PARTE" in
  banco)
    VPC="$(output_remoto k8s/network/terraform.tfstate vpc_id)"
    SUBNETS="$(output_remoto k8s/network/terraform.tfstate private_subnet_ids)"
    SG_APP="$(output_remoto k8s/network/terraform.tfstate application_security_group_id)"
    exigir "$VPC" "Rede não encontrada no bucket ${BUCKET} — rode antes o up.sh do autopecas-infra-k8s."
    cat <<EOF
# Gerado por infra/scripts/valores-aws.sh (repositório da aplicação) a partir
# das saídas do autopecas-infra-k8s. Muda a cada subida da rede.
environment = "${AMBIENTE}"
region      = "$(regiao)"

vpc_id                         = "${VPC}"
subnet_ids                     = ${SUBNETS}
application_security_group_ids = ["${SG_APP}"]

# Learner Lab: sem Multi-AZ nem backup, e destruível pelo down — a sessão dura
# horas, e o ambiente é recriado a cada uma.
instance_class        = "db.t3.micro"
multi_az              = false
backup_retention_days = 0
deletion_protection   = false
skip_final_snapshot   = true
EOF
    ;;

  lambda)
    SUBNETS="$(output_remoto k8s/network/terraform.tfstate private_subnet_ids)"
    VLINK="$(output_remoto k8s/cluster/terraform.tfstate vpc_link_id)"
    LISTENERS="$(output_remoto k8s/cluster/terraform.tfstate listener_arns)"
    DB_URL="$(output_remoto "db/${AMBIENTE}/terraform.tfstate" db_url)"
    SG_LAMBDA="$(output_remoto "db/${AMBIENTE}/terraform.tfstate" lambda_security_group_ids)"
    exigir "$VLINK" "Cluster não encontrado no bucket ${BUCKET} — rode antes o up.sh do autopecas-infra-k8s."
    exigir "$DB_URL" "Banco de ${AMBIENTE} não encontrado — aplique antes o autopecas-infra-db."
    # listener_arns é um mapa ambiente → ARN, em JSON numa linha só.
    LISTENER="$(printf '%s' "$LISTENERS" | sed -n "s/.*\"${AMBIENTE}\" *: *\"\([^\"]*\)\".*/\1/p")"
    exigir "$LISTENER" "Sem listener do NLB para ${AMBIENTE} na saída listener_arns."
    CONTA="${BUCKET##*-}"
    cat <<EOF
# Gerado por infra/scripts/valores-aws.sh (repositório da aplicação) a partir
# das saídas do autopecas-infra-k8s e do autopecas-infra-db. Muda a cada subida.
environment = "${AMBIENTE}"
region      = "$(regiao)"

# Learner Lab: a única role disponível. Em outra conta, troque pela role de
# execução da Lambda.
lambda_execution_role_arn = "${LAMBDA_ROLE_ARN:-arn:aws:iam::${CONTA}:role/LabRole}"
lambda_subnet_ids         = ${SUBNETS}
lambda_security_group_ids = ${SG_LAMBDA}
db_url                    = "${DB_URL}"
db_user                   = "lambda_auth"

# Integração privada com a aplicação: API Gateway → VPC Link → NLB → NodePort.
vpc_link_id              = "${VLINK}"
application_listener_arn = "${LISTENER}"

# GET /actuator/health sem JWT, para o monitor de disponibilidade do New Relic.
public_health_check = true
EOF
    ;;

  banco-backend|lambda-backend)
    PREFIXO="db"
    if [ "$PARTE" = "lambda-backend" ]; then
      PREFIXO="lambda"
    fi
    cat <<EOF
bucket       = "${BUCKET}"
key          = "${PREFIXO}/${AMBIENTE}/terraform.tfstate"
region       = "$(regiao)"
encrypt      = true
use_lockfile = true
EOF
    ;;

  *)
    morrer "Uso: AMBIENTE=<homologacao|producao> $0 <banco|banco-backend|lambda|lambda-backend>"
    ;;
esac
