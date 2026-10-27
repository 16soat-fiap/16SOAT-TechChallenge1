variable "aws_region" {
  description = "Região da AWS. Precisa ser a mesma da stack 01."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefixo de todos os recursos. Precisa ser o MESMO nas quatro stacks."
  type        = string
  default     = "autopecas"
}

variable "state_bucket" {
  description = <<-EOT
    Bucket S3 do state, criado pela stack _backend.

    Repetido como variável (além de estar no backend.hcl) porque o
    `terraform_remote_state` que lê a stack 01 é um data source comum, e data
    source não tem acesso à configuração do backend.
  EOT
  type        = string
}

# Declarada mas NÃO usada aqui — ver a explicação em 01-network/variables.tf.
# Existe para que um único terraform.tfvars sirva às quatro stacks.
variable "cluster_admin_role_arns" {
  description = "Usada pela stack 03. Aceita aqui apenas para compartilhar o tfvars."
  type        = list(string)
  default     = []
}

variable "engine_version" {
  description = "Major do PostgreSQL. Só a major — a AWS escolhe a minor."
  type        = string
  default     = "16"
}

variable "instance_class" {
  description = <<-EOT
    Classe da instância.

    db.t4g.micro (Graviton) está no Free Tier por 12 meses e é ~20% mais barata
    que a t3.micro equivalente depois disso. 2 vCPU burstable e 1 GB de RAM
    atendem a carga de uma demonstração; sob carga sustentada os créditos de CPU
    acabam e a latência sobe — se for fazer teste de carga pesado, suba para
    db.t4g.small.
  EOT
  type        = string
  default     = "db.t4g.micro"
}

variable "allocated_storage" {
  description = "Armazenamento inicial em GB. 20 é o mínimo do Free Tier."
  type        = number
  default     = 20

  validation {
    condition     = var.allocated_storage >= 20
    error_message = "allocated_storage mínimo do RDS PostgreSQL é 20 GB."
  }
}

variable "max_allocated_storage" {
  description = "Teto do autoscaling de armazenamento. Crescimento é irreversível."
  type        = number
  default     = 50
}

variable "database_name" {
  description = "Banco da aplicação. Mantém o mesmo nome do ambiente local."
  type        = string
  default     = "app_db"
}

variable "database_username" {
  description = "Usuário master. A senha é gerada e rotacionada pelo próprio RDS."
  type        = string
  default     = "autopecas"
}

variable "multi_az" {
  description = <<-EOT
    Réplica síncrona em outra AZ, com failover automático.

    Desligado por padrão: dobra o custo da instância e o Free Tier não cobre.
    Em produção seria `true` sem pensar duas vezes.
  EOT
  type        = bool
  default     = false
}

variable "backup_retention_period" {
  description = <<-EOT
    Dias de retenção de backup. Zero DESLIGA o backup automático e, com ele, o
    point-in-time recovery — não use zero só para economizar, o custo de backup
    até o tamanho do banco é gratuito.
  EOT
  type        = number
  default     = 7

  validation {
    condition     = var.backup_retention_period >= 1 && var.backup_retention_period <= 35
    error_message = "backup_retention_period deve estar entre 1 e 35 dias (zero desligaria o PITR)."
  }
}

variable "deletion_protection" {
  description = "Impede destruir a instância. `false` aqui porque o ambiente é descartável."
  type        = bool
  default     = false
}

variable "skip_final_snapshot" {
  description = "Pula o snapshot final no destroy. `true` evita custo de armazenamento residual."
  type        = bool
  default     = true
}
