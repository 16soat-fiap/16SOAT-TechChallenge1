variable "aws_region" {
  description = "Região onde o ambiente é provisionado. Ver _backend/variables.tf."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefixo dos recursos. Precisa ser o mesmo das outras stacks: a política do operador só alcança o que começa com ele."
  type        = string
  default     = "autopecas"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,30}$", var.project_name))
    error_message = "project_name deve ter 3-31 caracteres, começar com letra e usar apenas minúsculas, dígitos e hífen."
  }
}

variable "allowed_regions" {
  description = <<-EOT
    Regiões em que o operador pode criar recursos.

    Fora delas, tudo é negado (exceto serviços globais como IAM e STS). O motivo
    é custo, não segurança: um apply com a região errada cria um ambiente que
    ninguém procura, e que continua cobrando.
  EOT
  type        = list(string)
  default     = ["us-east-1"]
}

variable "budget_limit_usd" {
  description = <<-EOT
    Limite mensal do budget, em dólares, para a CONTA inteira.

    Não é teto de gasto: a AWS não tem teto rígido. O budget só avisa, e a
    avaliação acontece ~1 vez por dia, então o alerta pode chegar com até 24h de
    atraso. Com o ambiente a ~US$ 6/dia, US$ 20 cabem ~3 dias ligados no mês.
  EOT
  type        = number
  default     = 20

  validation {
    condition     = var.budget_limit_usd > 0
    error_message = "budget_limit_usd deve ser maior que zero."
  }
}

variable "budget_email" {
  description = "E-mail que recebe os alertas do budget. Sem default de propósito: um default errado manda os alertas para ninguém."
  type        = string

  validation {
    condition     = can(regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", var.budget_email))
    error_message = "budget_email deve ser um endereço de e-mail válido."
  }
}
