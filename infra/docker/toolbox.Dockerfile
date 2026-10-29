# =============================================================================
# Toolbox — Terraform + AWS CLI + kubectl numa imagem só.
#
# Por que existe: as stacks 03 e 04 autenticam no cluster com
#
#     exec { command = "aws" ... }
#
# ou seja, o Terraform INVOCA o AWS CLI em tempo de apply. A imagem oficial
# `hashicorp/terraform` não traz o AWS CLI (verificado: `which aws` falha), nem
# kubectl. Rodar `terraform apply` nela quebra com "exec: aws: executable file
# not found in $PATH" — e só nas stacks 03 e 04, depois de o `validate` e o
# `plan` das duas primeiras terem passado, o que torna o diagnóstico confuso.
#
# Com esta imagem, a única dependência da máquina é o Docker. Importa para quem
# vai avaliar o projeto: não precisa instalar Terraform, AWS CLI nem kubectl
# para subir o ambiente.
#
# ─── Por que Debian e não a base Alpine do Terraform ─────────────────────────
#
# A versão anterior partia de `hashicorp/terraform` e instalava o `aws-cli` do
# Alpine — a 2.22.10, anterior ao `aws login` (exige >= 2.32). Com ela, um
# profile criado por `aws login`, que é o login recomendado hoje pela AWS para
# quem usa o console, simplesmente não autentica dentro do container.
#
# O instalador oficial do AWS CLI v2 é compilado contra glibc e não roda no
# Alpine (musl). Daí a base Debian, com as três ferramentas em versão fixada.
#
# Construir:
#   docker build -f infra/docker/toolbox.Dockerfile -t autopecas-toolbox:local .
#
# Ou simplesmente usar os scripts em infra/scripts/, que a constroem sozinhos.
# =============================================================================
FROM debian:12.11-slim

# bash porque os scripts usam `pipefail` e arrays.
# jq para ler os outputs do Terraform em JSON sem recorrer a grep.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
         bash ca-certificates curl git jq unzip \
    && rm -rf /var/lib/apt/lists/*

# Mesma versão de antes: o state e o .terraform.lock.hcl das stacks foram
# gerados com ela.
ARG TERRAFORM_VERSION=1.10.5
RUN curl -fsSLo /tmp/terraform.zip \
      "https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip" \
    && unzip -q /tmp/terraform.zip -d /usr/local/bin \
    && rm /tmp/terraform.zip \
    && terraform version

# >= 2.32 para o `aws login`. Instalador oficial, versão fixada.
ARG AWSCLI_VERSION=2.38.5
RUN curl -fsSLo /tmp/awscli.zip \
      "https://awscli.amazonaws.com/awscli-exe-linux-x86_64-${AWSCLI_VERSION}.zip" \
    && unzip -q /tmp/awscli.zip -d /tmp \
    && /tmp/aws/install \
    && rm -rf /tmp/awscli.zip /tmp/aws \
    && aws --version

# Versão fixada e alinhada com `kubernetes_version` da stack 03. O kubectl tem
# compatibilidade de ±1 minor com o servidor, então 1.31 cobre de 1.30 a 1.32.
ARG KUBECTL_VERSION=v1.31.4
RUN curl -fsSLo /usr/local/bin/kubectl \
      "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl" \
    && chmod 0755 /usr/local/bin/kubectl \
    && kubectl version --client=true

# Ponte entre o `aws login` e o Terraform — ver o próprio script.
COPY infra/docker/toolbox-entrypoint.sh /usr/local/bin/toolbox-entrypoint
RUN chmod 0755 /usr/local/bin/toolbox-entrypoint

ENTRYPOINT ["/usr/local/bin/toolbox-entrypoint"]
CMD ["/bin/bash"]

WORKDIR /repo
