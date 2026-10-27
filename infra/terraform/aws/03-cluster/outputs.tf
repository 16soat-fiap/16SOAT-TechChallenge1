output "cluster_name" {
  description = "Nome do cluster. Usado em `aws eks update-kubeconfig`."
  value       = aws_eks_cluster.main.name
}

output "cluster_endpoint" {
  description = "Endpoint da API do Kubernetes."
  value       = aws_eks_cluster.main.endpoint
}

output "cluster_version" {
  description = "Versão do Kubernetes em execução."
  value       = aws_eks_cluster.main.version
}

output "cluster_security_group_id" {
  description = "Security group que o EKS criou para o control plane."
  value       = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
}

output "node_group_asg_name" {
  description = "Auto Scaling Group dos nós — o que o Cluster Autoscaler redimensiona."
  value       = aws_eks_node_group.main.resources[0].autoscaling_groups[0].name
}

output "nlb_arn" {
  description = "ARN do NLB interno."
  value       = aws_lb.internal.arn
}

output "nlb_dns_name" {
  description = "DNS interno do NLB. Resolve apenas de dentro da VPC."
  value       = aws_lb.internal.dns_name
}

output "listener_arns" {
  description = <<-EOT
    ARN do listener de cada serviço, indexado pela chave do mapa `node_ports`.

    É o que a stack 04 integra ao API Gateway: uma integração HTTP privada
    aponta para um ARN de listener, não para o balanceador.
  EOT
  value       = { for k, v in aws_lb_listener.services : k => v.arn }
}

output "kubeconfig_command" {
  description = "Comando para apontar o kubectl local para este cluster."
  value       = "aws eks update-kubeconfig --name ${aws_eks_cluster.main.name} --region ${var.aws_region}"
}
