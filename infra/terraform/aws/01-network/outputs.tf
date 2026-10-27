# =============================================================================
# Estes outputs são a interface pública desta stack: as stacks 02, 03 e 04 os
# leem por `terraform_remote_state`. Remover ou renomear um output quebra as
# stacks seguintes no próximo plan — trate como API.
# =============================================================================

output "vpc_id" {
  description = "ID da VPC."
  value       = aws_vpc.main.id
}

output "vpc_cidr" {
  description = "CIDR da VPC."
  value       = aws_vpc.main.cidr_block
}

output "public_subnet_ids" {
  description = "Subnets públicas (apenas o NAT Gateway mora nelas)."
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Subnets privadas: nós do EKS, RDS, NLB interno e VPC Link."
  value       = aws_subnet.private[*].id
}

output "availability_zones" {
  description = "AZs em uso."
  value       = local.azs
}

output "cluster_name" {
  description = <<-EOT
    Nome do cluster EKS, derivado de project_name.

    Fica nesta stack para que as demais usem a MESMA derivação sem repetir a
    expressão — se cada stack montasse o nome por conta própria, uma divergência
    só apareceria como "cluster não encontrado" na hora do apply.
  EOT
  value       = local.cluster_name
}

output "security_group_ids" {
  description = "Security groups da cadeia de tráfego."
  value = {
    nodes   = aws_security_group.nodes.id
    nlb     = aws_security_group.nlb.id
    vpclink = aws_security_group.vpclink.id
    rds     = aws_security_group.rds.id
  }
}

output "node_ports" {
  description = <<-EOT
    Mapa dos serviços expostos pelo cluster: porta do listener no NLB, NodePort
    e caminho de health check.

    A stack 03 itera sobre este mapa para criar um target group e um listener
    por serviço — acrescentar um terceiro serviço exposto é acrescentar uma
    entrada aqui, e nada mais.
  EOT
  value       = local.node_ports
}
