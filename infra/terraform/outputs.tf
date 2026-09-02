output "cluster_name" {
  description = "Nome do cluster kind criado."
  value       = kind_cluster.autopecas.name
}

output "kubeconfig_path" {
  description = "Caminho do kubeconfig escrito pelo kind."
  value       = kind_cluster.autopecas.kubeconfig_path
}

output "kubectl_context" {
  description = "Contexto do kubectl para este cluster."
  value       = "kind-${kind_cluster.autopecas.name}"
}

output "namespace" {
  description = "Namespace provisionado para a aplicação."
  value       = kubernetes_namespace.autopecas.metadata[0].name
}

output "database_jdbc_url" {
  description = "URL JDBC do banco da aplicação, como vista de dentro do cluster."
  value       = "jdbc:postgresql://postgres:5432/${var.app_database}"
}

output "keycloak_jdbc_url" {
  description = "URL JDBC do banco do Keycloak, como vista de dentro do cluster."
  value       = "jdbc:postgresql://postgres:5432/${var.keycloak_database}"
}

output "app_url" {
  description = "URL da API a partir da máquina host (após aplicar infra/k8s)."
  value       = "http://localhost:${var.app_host_port}"
}

output "keycloak_url" {
  description = "URL do Keycloak a partir da máquina host (após aplicar infra/k8s)."
  value       = "http://localhost:${var.keycloak_host_port}"
}

output "proximos_passos" {
  description = "O que fazer depois que o Terraform terminar."
  value       = <<-EOT

    Cluster e banco prontos. Para subir a aplicação:

      1. Construir a imagem:
         docker build -t autopecas-api:local .

      2. Carregar a imagem no cluster (kind não usa registry):
         kind load docker-image autopecas-api:local --name ${kind_cluster.autopecas.name}

      3. Aplicar os manifestos:
         kubectl apply -f infra/k8s/

      4. Acompanhar:
         kubectl get pods -n ${var.namespace} -w
         kubectl get hpa  -n ${var.namespace} -w

      API:      http://localhost:${var.app_host_port}/swagger-ui.html
      Keycloak: http://localhost:${var.keycloak_host_port}  (admin/admin)
  EOT
}
