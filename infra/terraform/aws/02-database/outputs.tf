output "db_endpoint" {
  description = "Host:porta da instância."
  value       = aws_db_instance.main.endpoint
}

output "db_address" {
  description = "Apenas o hostname."
  value       = aws_db_instance.main.address
}

output "db_port" {
  description = "Porta."
  value       = aws_db_instance.main.port
}

output "db_name" {
  description = "Nome do banco da aplicação."
  value       = aws_db_instance.main.db_name
}

output "db_username" {
  description = "Usuário master."
  value       = aws_db_instance.main.username
}

output "jdbc_url" {
  description = <<-EOT
    URL JDBC pronta, com `sslmode=require` — obrigatório porque o parameter
    group aplica `rds.force_ssl = 1`. Vai direto para o ConfigMap na stack 04.
  EOT
  value       = "jdbc:postgresql://${aws_db_instance.main.endpoint}/${aws_db_instance.main.db_name}?sslmode=require"
}

output "master_user_secret_arn" {
  description = <<-EOT
    ARN do secret gerenciado pelo RDS, no Secrets Manager.

    Só o ARN sai daqui, nunca o valor: a senha não existe neste state. A stack
    04 lê o conteúdo do secret para montar o Secret do Kubernetes.
  EOT
  value       = aws_db_instance.main.master_user_secret[0].secret_arn
}
