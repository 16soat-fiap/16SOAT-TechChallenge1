variable "account_id" {
  description = "ID da conta do New Relic (número que aparece na URL do painel)."
  type        = number
}

variable "region" {
  description = "Região de dados da conta do New Relic: US ou EU."
  type        = string
  default     = "US"

  validation {
    condition     = contains(["US", "EU"], var.region)
    error_message = "region deve ser US ou EU."
  }
}

variable "email_alertas" {
  description = "Quem recebe os alertas por e-mail."
  type        = string
}

variable "cluster_name" {
  description = "Nome do cluster EKS, como a integração do Kubernetes o reporta (global.cluster do nri-bundle)."
  type        = string
  default     = "autopecas-eks"
}

# Um dashboard, um conjunto de alertas e um monitor de disponibilidade por
# ambiente. `health_url` nulo deixa o ambiente sem monitor sintético — o caso de
# um ambiente cujo gateway não está de pé quando a stack é aplicada.
variable "ambientes" {
  description = "Ambientes monitorados, com a URL pública do healthcheck da aplicação."
  type = map(object({
    health_url = optional(string)
  }))
  default = {
    homologacao = {}
    producao    = {}
  }

  validation {
    condition     = alltrue([for nome in keys(var.ambientes) : contains(["homologacao", "producao"], nome)])
    error_message = "Os ambientes são homologacao e producao."
  }
}

# ─────────────────────────────────────────────────────────────────────────────
# Limiares dos alertas. Ficam aqui, e não espalhados nas condições, para serem
# ajustados num lugar só depois de observar o tráfego real.
# ─────────────────────────────────────────────────────────────────────────────

variable "latencia_p95_limite_segundos" {
  description = "Latência p95 da API acima da qual o alerta dispara (5 minutos seguidos)."
  type        = number
  default     = 2
}

variable "taxa_erro_limite_percentual" {
  description = "Percentual de respostas 5xx acima do qual o alerta dispara (5 minutos seguidos)."
  type        = number
  default     = 5
}
