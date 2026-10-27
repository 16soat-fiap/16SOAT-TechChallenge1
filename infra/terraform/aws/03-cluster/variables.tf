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

variable "kubernetes_version" {
  description = <<-EOT
    Versão do Kubernetes do control plane.

    Uma versão abaixo da mais recente, de propósito: a mais nova às vezes ainda
    não tem todas as versões de addon publicadas, e o apply falha ao resolver a
    compatibilidade. Cada versão tem ~14 meses de suporte padrão — depois a AWS
    cobra extra pelo suporte estendido.
  EOT
  type        = string
  default     = "1.31"
}

variable "public_access_cidrs" {
  description = <<-EOT
    Quem pode alcançar o endpoint da API do Kubernetes.

    Aberto por padrão porque o runner do GitHub Actions não tem IP fixo. O
    endpoint exige autenticação IAM — não é acesso anônimo —, mas restringir
    por CIDR seria uma camada a mais. Note que isto NÃO expõe a aplicação: ver
    o comentário no topo de eks.tf.
  EOT
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "cluster_admin_role_arns" {
  description = <<-EOT
    ARNs de papéis IAM que recebem admin no cluster, além de quem rodou o apply.

    Tipicamente o papel que o GitHub Actions assume via OIDC. Sem uma entrada
    aqui, todo `kubectl` do pipeline devolve 401 — e a mensagem não diz que o
    problema é access entry.
  EOT
  type        = list(string)
  default     = []
}

variable "log_retention_days" {
  description = "Retenção do log do control plane no CloudWatch."
  type        = number
  default     = 7
}

# ─────────────────────────────────────────────────────────────────────────────
# Dimensionamento dos nós — a aritmética está comentada em nodes.tf
# ─────────────────────────────────────────────────────────────────────────────

variable "node_instance_type" {
  description = <<-EOT
    Tipo de instância dos nós.

    t3.medium (2 vCPU / 4 GiB) é o menor tipo em que o autoscaling é
    demonstrável: com t3.small sobra espaço para UMA réplica da API por nó, e
    cada réplica nova do HPA exigiria um nó novo (~2 min de espera). Ver a conta
    completa em nodes.tf.
  EOT
  type        = string
  default     = "t3.medium"
}

variable "node_desired_size" {
  description = <<-EOT
    Tamanho inicial da frota.

    Só vale no PRIMEIRO apply: depois disso o Cluster Autoscaler assume o
    controle e o Terraform ignora este valor (ver o `ignore_changes` em
    nodes.tf).
  EOT
  type        = number
  default     = 2
}

variable "node_min_size" {
  description = <<-EOT
    Piso da frota.

    Dois, para que as réplicas da API fiquem em nós diferentes — o
    topologySpreadConstraints do Deployment precisa de ao menos dois nós para
    ter onde distribuir.
  EOT
  type        = number
  default     = 2
}

variable "node_max_size" {
  description = "Teto da frota. Limita o gasto máximo que o autoscaling pode gerar."
  type        = number
  default     = 4

  validation {
    condition     = var.node_max_size >= var.node_min_size
    error_message = "node_max_size precisa ser maior ou igual a node_min_size."
  }
}

# ─────────────────────────────────────────────────────────────────────────────
# Versões de chart fixadas: um chart que muda sozinho faz o apply de amanhã
# instalar algo diferente do que foi testado hoje.
# ─────────────────────────────────────────────────────────────────────────────

variable "metrics_server_chart_version" {
  description = "Versão do chart do metrics-server."
  type        = string
  default     = "3.12.2"
}

variable "cluster_autoscaler_chart_version" {
  description = "Versão do chart do Cluster Autoscaler."
  type        = string
  default     = "9.46.6"
}
