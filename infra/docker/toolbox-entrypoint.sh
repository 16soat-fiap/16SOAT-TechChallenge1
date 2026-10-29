#!/usr/bin/env bash
# =============================================================================
# Entrypoint do toolbox: deixa o Terraform usar um profile do `aws login`.
#
# O AWS CLI da imagem entende `login_session` nativamente. O provider da AWS
# do Terraform (5.x) não: com um profile desses ele falha com "no valid
# credential sources found".
#
# Exportar a credencial uma vez e passá-la por variável de ambiente NÃO serve:
# o `aws login` emite credenciais de 15 minutos, e um `apply` da stack 03 leva
# ~20. O apply cairia no meio da criação do cluster — já cobrando.
#
# A saída documentada pela AWS é o `credential_process`: um profile que pede a
# credencial ao AWS CLI sempre que a anterior expira. Este script cria esse
# profile numa cópia do config (o original é montado só para leitura) e aponta
# o Terraform para ele. O CLI renova a sessão usando o cache em
# ~/.aws/login/cache, que os scripts montam com escrita.
#
# Só age quando é o Terraform rodando, sem credencial em variável de ambiente,
# e o profile em uso é de `aws login`. Em qualquer outro caso — chave de
# acesso, SSO, credencial de laboratório —, não faz nada.
# =============================================================================
set -euo pipefail

perfil="${AWS_PROFILE:-default}"

if [ "${1:-}" = "terraform" ] \
   && [ -z "${AWS_ACCESS_KEY_ID:-}" ] \
   && [ -f "${HOME}/.aws/config" ] \
   && aws configure get login_session --profile "$perfil" >/dev/null 2>&1; then

  regiao_perfil="$(aws configure get region --profile "$perfil" 2>/dev/null || true)"

  mkdir -p /tmp/aws
  cp "${HOME}/.aws/config" /tmp/aws/config
  cat >> /tmp/aws/config <<EOF

[profile ${perfil}-processo]
credential_process = aws configure export-credentials --profile ${perfil} --format process
region = ${regiao_perfil:-us-east-1}
EOF

  export AWS_CONFIG_FILE=/tmp/aws/config
  export AWS_PROFILE="${perfil}-processo"
fi

exec "$@"
