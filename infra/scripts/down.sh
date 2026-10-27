#!/usr/bin/env bash
# =============================================================================
# Destrói a infraestrutura, na ordem inversa.
#
#   ./infra/scripts/down.sh            # derruba o cluster e o gateway (~US$ 149/mês)
#   ./infra/scripts/down.sh --tudo     # derruba também a rede e o BANCO (apaga os dados)
#
# Por que o padrão NÃO destrói tudo: as stacks 03 e 04 respondem por ~US$ 149
# dos ~US$ 183/mês, e são as únicas que levam ~30 min para voltar. Preservar a
# rede e o banco deixa o ambiente "dormindo" por ~US$ 34/mês, com os dados
# intactos e um `up.sh` de volta.
#
# `--tudo` apaga a instância RDS. Com `skip_final_snapshot = true` (o padrão
# desta configuração), NÃO há snapshot: os dados vão embora de verdade.
# =============================================================================

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

TUDO="nao"
if [ "${1:-}" = "--tudo" ]; then
  TUDO="sim"
fi

exigir_aws_configurada

TFVARS="$(caminho_tfvars)"
if [ ! -f "${REPO_ROOT}/${TF_SUBDIR}/terraform.tfvars" ]; then
  morrer "terraform.tfvars não encontrado. Este ambiente foi provisionado por outro caminho?"
fi

# Ordem inversa: o gateway referencia o NLB do cluster, o cluster referencia a
# rede. Destruir a rede primeiro deixaria recursos órfãos que o Terraform não
# consegue mais encontrar.
A_DESTRUIR=(04-gateway-auth 03-cluster)
if [ "$TUDO" = "sim" ]; then
  A_DESTRUIR+=(02-database 01-network)
fi

titulo "Stacks que serão destruídas"
for stack in "${A_DESTRUIR[@]}"; do
  echo "    ${stack}"
done

if [ "$TUDO" = "sim" ]; then
  echo
  aviso "A stack 02-database será destruída: a instância RDS e TODOS OS DADOS vão embora."
  aviso "Não haverá snapshot final (skip_final_snapshot = true)."
fi

echo
printf 'Confirma? Digite "destruir" para prosseguir: '
read -r RESPOSTA
if [ "$RESPOSTA" != "destruir" ]; then
  echo "Cancelado."
  exit 0
fi

for stack in "${A_DESTRUIR[@]}"; do
  titulo "Destruindo ${stack}"

  # O init é necessário porque o diretório pode estar limpo (clone novo, ou
  # .terraform apagado) e sem ele o Terraform não sabe onde está o state.
  tf_stack "$stack" init -input=false -reconfigure \
    -backend-config="$(caminho_backend_hcl)"

  tf_stack "$stack" destroy -input=false -auto-approve \
    -var-file="$TFVARS"

  ok "${stack} destruída"
done

titulo "Pronto"
if [ "$TUDO" = "sim" ]; then
  cat <<'EOF'

  Tudo destruído, exceto a stack _backend — de propósito: ela guarda o bucket
  de state (com `prevent_destroy`) e o ECR com as imagens já publicadas.
  Custo residual: centavos de armazenamento.

  Para remover também, o bucket precisa ser esvaziado e o `prevent_destroy`
  retirado à mão. Normalmente não vale a pena: é o que permite subir o ambiente
  de novo sem recomeçar do zero.

EOF
else
  cat <<'EOF'

  Rede e banco preservados — o ambiente está "dormindo" por ~US$ 34/mês
  (NAT Gateway + RDS fora do Free Tier), com os dados intactos.

  Para voltar:            ./infra/scripts/up.sh && ./infra/scripts/deploy.sh
  Para destruir o resto:  ./infra/scripts/down.sh --tudo

EOF
fi
