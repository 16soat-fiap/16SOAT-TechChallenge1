# =============================================================================
# Security groups — a cadeia de confiança do tráfego de entrada.
#
#   API Gateway ──► VPC Link ──► NLB interno ──► nós do EKS ──► RDS
#      (público)     sg_vpclink    sg_nlb          sg_nodes      sg_rds
#
# Cada elo libera a porta APENAS para o security group anterior, nunca para um
# CIDR. A diferença é prática: regra por CIDR continua valendo se alguém criar
# outra instância naquela faixa; regra por security group vale só para quem
# tem aquele SG anexado. Em VPC pequena isso parece detalhe, mas é o que
# garante que o único caminho até a aplicação seja o API Gateway.
#
# As regras usam `aws_vpc_security_group_*_rule` (um recurso por regra) e não
# blocos `ingress`/`egress` inline: com blocos inline, uma regra adicionada por
# outra stack — como as do control plane do EKS, na stack 03 — seria removida
# no apply seguinte, porque o Terraform trata a lista inline como o conjunto
# completo e autoritativo.
# =============================================================================

# ─────────────────────────────────────────────────────────────────────────────
# Nós do EKS
# ─────────────────────────────────────────────────────────────────────────────
resource "aws_security_group" "nodes" {
  # `name_prefix` e não `name`: com nome fixo, uma substituição do security
  # group falharia com InvalidGroup.Duplicate, porque o `create_before_destroy`
  # abaixo tenta criar o novo enquanto o antigo ainda existe — e nome de
  # security group é único na VPC. O prefixo deixa a AWS sortear o sufixo.
  #
  # O nome legível continua na tag `Name`, que é o que aparece no console.
  name_prefix = "${var.project_name}-nodes-"
  description = "Nos do cluster EKS"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-nodes"
  }

  # O SG é referenciado pelo launch template do node group (stack 03). Trocar o
  # SG exige recriar o template, então cria-se o novo antes de destruir o antigo.
  lifecycle {
    create_before_destroy = true
  }
}

# Pod para pod entre nós diferentes, e kubelet para kubelet. Sem isto, qualquer
# chamada que atravesse nós falha — inclusive o CoreDNS, o que quebra o cluster
# de forma difícil de diagnosticar (nomes resolvem às vezes, dependendo de em
# qual nó o pod de DNS caiu).
resource "aws_vpc_security_group_ingress_rule" "nodes_self" {
  security_group_id            = aws_security_group.nodes.id
  referenced_security_group_id = aws_security_group.nodes.id
  ip_protocol                  = "-1"
  description                  = "Trafego entre nos do cluster"
}

# Os dois NodePorts expostos pelo cluster: a API e o Keycloak. É por aqui que o
# NLB entrega o tráfego — e o único jeito de alcançar qualquer um dos dois de
# fora do cluster.
resource "aws_vpc_security_group_ingress_rule" "nodes_from_nlb" {
  for_each = local.node_ports

  security_group_id            = aws_security_group.nodes.id
  referenced_security_group_id = aws_security_group.nlb.id
  ip_protocol                  = "tcp"
  from_port                    = each.value.node_port
  to_port                      = each.value.node_port
  description                  = "NodePort do ${each.key}, apenas a partir do NLB interno"
}

# Saída liberada: os nós baixam imagem do ECR, resolvem DNS público, falam com
# a API do EKS e enviam log para o CloudWatch. Restringir a saída de um nó
# Kubernetes por porta/destino é possível, mas exige mapear todos os endpoints
# da AWS e quebra a cada serviço novo que se usa.
resource "aws_vpc_security_group_egress_rule" "nodes_all" {
  security_group_id = aws_security_group.nodes.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
  description       = "Saida liberada (ECR, EKS API, CloudWatch)"
}

# ─────────────────────────────────────────────────────────────────────────────
# NLB interno
#
# NLB com security group é possível desde 2023 — antes, um NLB não tinha SG e a
# filtragem tinha que acontecer no SG do destino. Com SG no NLB a cadeia fica
# completa e explícita.
# ─────────────────────────────────────────────────────────────────────────────
resource "aws_security_group" "nlb" {
  name_prefix = "${var.project_name}-nlb-"
  description = "NLB interno, alvo do VPC Link do API Gateway"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-nlb"
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "nlb_from_vpclink" {
  for_each = local.node_ports

  security_group_id            = aws_security_group.nlb.id
  referenced_security_group_id = aws_security_group.vpclink.id
  ip_protocol                  = "tcp"
  from_port                    = each.value.listener_port
  to_port                      = each.value.listener_port
  description                  = "Listener do ${each.key}, apenas a partir do VPC Link"
}

resource "aws_vpc_security_group_egress_rule" "nlb_to_nodes" {
  for_each = local.node_ports

  security_group_id            = aws_security_group.nlb.id
  referenced_security_group_id = aws_security_group.nodes.id
  ip_protocol                  = "tcp"
  from_port                    = each.value.node_port
  to_port                      = each.value.node_port
  description                  = "Encaminhamento e health check do ${each.key}"
}

# ─────────────────────────────────────────────────────────────────────────────
# VPC Link do API Gateway
#
# O VPC Link é gerenciado pela AWS, mas as ENIs que ele cria na nossa VPC usam
# ESTE security group. Só precisa de saída: ele nunca recebe conexão de dentro
# da VPC, apenas origina chamadas em direção ao NLB.
# ─────────────────────────────────────────────────────────────────────────────
resource "aws_security_group" "vpclink" {
  name_prefix = "${var.project_name}-vpclink-"
  description = "ENIs do VPC Link do API Gateway"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-vpclink"
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_egress_rule" "vpclink_to_nlb" {
  for_each = local.node_ports

  security_group_id            = aws_security_group.vpclink.id
  referenced_security_group_id = aws_security_group.nlb.id
  ip_protocol                  = "tcp"
  from_port                    = each.value.listener_port
  to_port                      = each.value.listener_port
  description                  = "Saida para o listener do ${each.key} no NLB"
}

# ─────────────────────────────────────────────────────────────────────────────
# RDS
#
# Nenhuma regra de saída: um banco de dados não origina conexões. O SG padrão
# da AWS liberaria tudo, por isso os recursos de regra explícitos — a ausência
# de `aws_vpc_security_group_egress_rule` aqui é intencional, não esquecimento.
# ─────────────────────────────────────────────────────────────────────────────
resource "aws_security_group" "rds" {
  name_prefix = "${var.project_name}-rds-"
  description = "PostgreSQL gerenciado"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-rds"
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "rds_from_nodes" {
  security_group_id            = aws_security_group.rds.id
  referenced_security_group_id = aws_security_group.nodes.id
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
  description                  = "PostgreSQL, apenas a partir dos nos do cluster"
}
