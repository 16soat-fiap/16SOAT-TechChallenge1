# =============================================================================
# Monitoramento no New Relic: dashboards, alertas e monitor de disponibilidade.
#
# Uma stack só, para os dois ambientes: tudo aqui é configuração do New Relic,
# nada roda na AWS. Aplicada por infra/scripts/observabilidade.sh, que gera o
# backend.hcl e o terraform.tfvars (não versionados) a partir das saídas dos
# outros repositórios.
#
# Quem coleta os dados não está aqui:
#   • a aplicação: agente Java na imagem (Dockerfile + infra/docker/newrelic.yml)
#     e os eventos de negócio da TelemetriaNewRelic;
#   • o cluster: chart nri-bundle no repositório autopecas-infra-k8s.
# =============================================================================
terraform {
  required_version = ">= 1.10"

  required_providers {
    newrelic = {
      source  = "newrelic/newrelic"
      version = "~> 3.101"
    }
  }

  # Mesmo bucket de state dos outros repositórios, com prefixo próprio. Bucket e
  # região chegam pelo backend.hcl gerado.
  backend "s3" {
    key          = "observabilidade/terraform.tfstate"
    encrypt      = true
    use_lockfile = true
  }
}

# A User API key (NRAK-...) vem da variável de ambiente NEW_RELIC_API_KEY, que o
# provider lê sozinho: não passa por tfvars, nem por argumento, nem pelo state.
provider "newrelic" {
  account_id = var.account_id
  region     = var.region
}
