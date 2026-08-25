variable "cluster_name" {
  description = "Nome do cluster kind."
  type        = string
  default     = "autopecas"
}

variable "node_image" {
  description = "Imagem dos nós do kind. Fixa a versão do Kubernetes provisionado."
  type        = string
  default     = "kindest/node:v1.31.0"
}

variable "worker_count" {
  description = "Quantidade de nós worker. Dois permitem observar o HPA distribuindo réplicas."
  type        = number
  default     = 2

  validation {
    condition     = var.worker_count >= 1 && var.worker_count <= 5
    error_message = "worker_count deve estar entre 1 e 5 — cada nó é um container Docker."
  }
}

variable "namespace" {
  description = "Namespace onde o banco e a aplicação são criados."
  type        = string
  default     = "autopecas"
}

variable "app_host_port" {
  description = "Porta da máquina host mapeada para o NodePort 30080 (API)."
  type        = number
  default     = 8080
}

variable "keycloak_host_port" {
  description = "Porta da máquina host mapeada para o NodePort 30081 (Keycloak)."
  type        = number
  default     = 9080
}

variable "postgres_image" {
  description = "Imagem do PostgreSQL."
  type        = string
  default     = "postgres:16-alpine"
}

variable "postgres_storage" {
  description = "Tamanho do volume persistente do banco."
  type        = string
  default     = "2Gi"
}

variable "postgres_user" {
  description = "Usuário do PostgreSQL."
  type        = string
  default     = "postgres"
}

variable "postgres_password" {
  description = <<-EOT
    Senha do PostgreSQL.

    O default existe para o ambiente local subir sem configuração. Em qualquer
    ambiente compartilhado, informe via `TF_VAR_postgres_password` — nunca em
    terraform.tfvars versionado, e lembrando que o valor fica em claro no
    arquivo de state.
  EOT
  type        = string
  default     = "postgres"
  sensitive   = true
}

variable "app_database" {
  description = "Banco da aplicação, gerenciado pelas migrations Flyway."
  type        = string
  default     = "app_db"
}

variable "keycloak_database" {
  description = "Banco do Keycloak, separado do banco da aplicação."
  type        = string
  default     = "keycloak_db"
}

variable "kubeconfig_path_override" {
  description = <<-EOT
    Caminho de um kubeconfig alternativo para os providers kubernetes/helm.

    Vazio (default) usa o kubeconfig que o próprio kind escreve. Serve para
    quando o Terraform NÃO roda na mesma rede em que o kubeconfig do kind
    aponta — por exemplo, executando o Terraform dentro de um container: lá o
    `127.0.0.1` do kubeconfig é o próprio container, não a máquina host.
  EOT
  type        = string
  default     = ""
}

variable "install_metrics_server" {
  description = "Instala o metrics-server. Sem ele o HPA não obtém métricas e fica em <unknown>."
  type        = bool
  default     = true
}
