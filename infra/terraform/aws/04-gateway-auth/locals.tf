data "terraform_remote_state" "network" {
  backend = "s3"

  config = {
    bucket = var.state_bucket
    key    = "01-network/terraform.tfstate"
    region = var.aws_region
  }
}

data "terraform_remote_state" "database" {
  backend = "s3"

  config = {
    bucket = var.state_bucket
    key    = "02-database/terraform.tfstate"
    region = var.aws_region
  }
}

data "terraform_remote_state" "cluster" {
  backend = "s3"

  config = {
    bucket = var.state_bucket
    key    = "03-cluster/terraform.tfstate"
    region = var.aws_region
  }
}

# O CA do cluster vem de um data source, e não do remote state: um certificado
# tem validade e é rotacionável, e lê-lo direto da API garante o valor atual.
data "aws_eks_cluster" "main" {
  name = local.cluster.cluster_name
}

# Credencial do banco, gerada e guardada pelo RDS (ver 02-database/main.tf).
#
# É aqui que a senha entra no Terraform — e, consequentemente, no state DESTA
# stack. Inevitável: alguém precisa materializar o Secret do Kubernetes. A
# alternativa que evitaria isso é o External Secrets Operator, que lê o Secrets
# Manager de dentro do cluster e dispensa o Terraform como intermediário; é a
# evolução natural daqui, ao custo de um operator a mais para manter.
data "aws_secretsmanager_secret_version" "db" {
  secret_id = local.database.master_user_secret_arn
}

locals {
  network  = data.terraform_remote_state.network.outputs
  database = data.terraform_remote_state.database.outputs
  cluster  = data.terraform_remote_state.cluster.outputs

  db_credenciais = jsondecode(data.aws_secretsmanager_secret_version.db.secret_string)

  # ───────────────────────────────────────────────────────────────────────────
  # A URL pública do API Gateway e o que ela determina.
  #
  # Este endereço é a identidade externa do sistema, e por isso aparece em três
  # lugares que PRECISAM concordar:
  #
  #   1. KC_HOSTNAME do Keycloak  → define o `iss` que ele escreve no token;
  #   2. ISSUER da Lambda         → o `iss` que ela exige encontrar;
  #   3. OAUTH2_ISSUER_URI da API → o `iss` que o Spring exige encontrar.
  #
  # Se divergirem, o token é recusado com "issuer inesperado" — e o diagnóstico
  # é chato porque o token parece perfeito. Daí os três saírem de um único
  # `local`.
  # ───────────────────────────────────────────────────────────────────────────
  api_base_url = aws_apigatewayv2_api.main.api_endpoint

  issuer_publico = "${local.api_base_url}/realms/${var.keycloak_realm}"

  # O JWKS, por outro lado, é buscado por DENTRO do cluster pela aplicação: o
  # Service do Keycloak resolve em `http://keycloak:8080`. Sai mais rápido e
  # não depende de o caminho público estar de pé.
  jwks_uri_interno = "http://keycloak:8080/realms/${var.keycloak_realm}/protocol/openid-connect/certs"

  # A Lambda, ao contrário, roda FORA da VPC e não alcança o Service do
  # Keycloak — então busca o JWKS pela rota pública /realms/*, que não passa
  # pelo authorizer (senão seria preciso um token para validar um token).
  #
  # O incômodo é real: a função chama o gateway que ela mesma protege. A
  # alternativa é anexar a Lambda às subnets privadas, e aí ela fala com o
  # Keycloak internamente; custa ENI no cold start e um bloco `vpc_config`.
  # Com o cache em memória, a chamada é rara, e ficou do lado simples.
  jwks_uri_lambda = "${local.issuer_publico}/protocol/openid-connect/certs"
}
