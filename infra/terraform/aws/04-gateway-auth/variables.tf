variable "aws_region" {
  description = "Região da AWS. Precisa ser a mesma das stacks anteriores."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefixo de todos os recursos. Precisa ser o MESMO nas quatro stacks."
  type        = string
  default     = "autopecas"
}

variable "state_bucket" {
  description = "Bucket S3 do state, criado pela stack _backend."
  type        = string
}

variable "namespace" {
  description = "Namespace do Kubernetes onde a aplicação roda."
  type        = string
  default     = "autopecas"
}

variable "log_retention_days" {
  description = "Retenção dos logs do gateway e da Lambda no CloudWatch."
  type        = number
  default     = 7
}

# Declarada mas NÃO usada aqui — ver a explicação em 01-network/variables.tf.
# Existe para que um único terraform.tfvars sirva às quatro stacks.
variable "cluster_admin_role_arns" {
  description = "Usada pela stack 03. Aceita aqui apenas para compartilhar o tfvars."
  type        = list(string)
  default     = []
}

# ─────────────────────────────────────────────────────────────────────────────
# Keycloak
# ─────────────────────────────────────────────────────────────────────────────

variable "keycloak_realm" {
  description = <<-EOT
    Nome do realm.

    Precisa bater com o `realm` dentro de keycloak/realm-export.json: o claim
    `iss` do token é "<url>/realms/<realm>", e divergir aqui faz todo token ser
    recusado com "issuer inesperado".
  EOT
  type        = string
  default     = "autopecas"
}

variable "keycloak_client_id" {
  description = "Client do realm. Usado para ler resource_access.<client>.roles."
  type        = string
  default     = "autopecas-api"
}

variable "keycloak_schema" {
  description = <<-EOT
    Schema do PostgreSQL onde o Keycloak cria as próprias tabelas.

    Separado do `public`, onde o Flyway e o `ddl-auto: validate` da aplicação
    trabalham. Criado por um initContainer no Deployment do Keycloak — ele não
    cria o schema sozinho.
  EOT
  type        = string
  default     = "keycloak"
}

variable "keycloak_admin_user" {
  description = "Usuário do console administrativo. A senha é gerada pelo Terraform."
  type        = string
  default     = "admin"
}

# ─────────────────────────────────────────────────────────────────────────────
# Authorizer
# ─────────────────────────────────────────────────────────────────────────────

variable "lambda_runtime" {
  description = <<-EOT
    Runtime da Lambda.

    nodejs22.x: o código usa `node:crypto` com importação de JWK
    (`format: 'jwk'`) e `AbortSignal.timeout`, disponíveis desde o Node 18.
  EOT
  type        = string
  default     = "nodejs22.x"
}

variable "authorizer_cache_ttl_seconds" {
  description = <<-EOT
    Quanto tempo o API Gateway reaproveita a decisão do authorizer para o mesmo
    token.

    Zero desliga o cache: a Lambda passa a ser invocada em TODA requisição —
    mais latência e mais custo, em troca de reagir imediatamente a um token
    revogado. 300s (o máximo) é o padrão porque revogação não é verificada em
    nenhum ponto deste sistema; quem limita a validade é o `exp` do token.
  EOT
  type        = number
  default     = 300

  validation {
    condition     = var.authorizer_cache_ttl_seconds >= 0 && var.authorizer_cache_ttl_seconds <= 3600
    error_message = "authorizer_cache_ttl_seconds deve estar entre 0 e 3600."
  }
}

variable "jwks_cache_ttl_seconds" {
  description = "Validade do cache do JWKS na memória da Lambda."
  type        = number
  default     = 600
}

variable "clock_skew_seconds" {
  description = <<-EOT
    Tolerância de dessincronização de relógio na checagem de `exp` e `nbf`.

    Sem tolerância, um token recém-emitido pode ser recusado por `nbf` no
    futuro — falha intermitente e difícil de reproduzir.
  EOT
  type        = number
  default     = 60
}

variable "jwt_audiences" {
  description = <<-EOT
    Audiências aceitas. Lista vazia DESLIGA a verificação.

    Vazia por padrão: o access token do Keycloak traz `aud: "account"`, e o
    resource server do Spring também não valida audiência
    (JwtValidators.createDefaultWithIssuer confere só o issuer). Exigi-la aqui
    recusaria todo token emitido pelo realm atual.
  EOT
  type        = list(string)
  default     = []
}

# ─────────────────────────────────────────────────────────────────────────────
# Gateway
# ─────────────────────────────────────────────────────────────────────────────

variable "throttling_rate_limit" {
  description = <<-EOT
    Requisições por segundo, em regime permanente, no stage.

    É a proteção mais barata desta infraestrutura: sem ela, uma rajada chega
    inteira ao cluster e o HPA responde escalando — ou seja, gastando.
  EOT
  type        = number
  default     = 100
}

variable "throttling_burst_limit" {
  description = "Teto instantâneo de requisições concorrentes no stage."
  type        = number
  default     = 200
}

variable "cors_allow_origins" {
  description = <<-EOT
    Origens permitidas no CORS.

    "*" por padrão para que Swagger e qualquer frontend de teste funcionem sem
    configuração. Com um frontend de verdade, liste o domínio dele: "*" permite
    que qualquer página na internet faça chamadas à API com o token da vítima,
    se ela tiver um.
  EOT
  type        = list(string)
  default     = ["*"]
}

variable "expose_swagger" {
  description = <<-EOT
    Publica as rotas do Swagger UI e do /v3/api-docs no gateway, sem authorizer.

    `true` ajuda na avaliação e na demonstração. Em produção seria `false`: o
    contrato da API não precisa ser legível por quem não a usa.
  EOT
  type        = bool
  default     = true
}
