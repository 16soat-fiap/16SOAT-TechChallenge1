# =============================================================================
# Rede — a base de tudo.
#
# Topologia em duas camadas, por motivo de segurança e não de organização:
#
#   públicas  → apenas o NAT Gateway. Nada da aplicação mora aqui.
#   privadas  → nós do EKS, RDS e o NLB interno. Sem rota de entrada da
#               internet: a ÚNICA porta externa do sistema é o API Gateway,
#               que chega até aqui por VPC Link (ver stack 04).
#
# É isso que faz o "API Gateway para controle e roteamento" ser real. Se o
# cluster tivesse um Ingress público, o gateway seria contornável e todo o
# controle de autenticação nele seria decorativo.
# =============================================================================

# Duas AZs é o mínimo que o EKS aceita para o control plane. Pegar as duas
# primeiras disponíveis (em vez de fixar "us-east-1a") evita quebrar em contas
# onde alguma AZ não está habilitada.
data "aws_availability_zones" "disponiveis" {
  state = "available"
}

locals {
  azs          = slice(data.aws_availability_zones.disponiveis.names, 0, 2)
  cluster_name = "${var.project_name}-eks"

  # ───────────────────────────────────────────────────────────────────────────
  # Os dois serviços que o cluster expõe ao API Gateway.
  #
  # Definidos aqui, e não na stack 03, porque as regras de security group
  # precisam deles — e porque assim existe UMA fonte da verdade para o par
  # (porta do listener no NLB, NodePort no cluster). Se cada stack escolhesse
  # as portas por conta própria, a divergência apareceria só como target group
  # permanentemente unhealthy, que é um sintoma sem relação óbvia com a causa.
  #
  # `health_path` é o endpoint que o NLB consulta. Os dois são liberados sem
  # autenticação: o da API está no HEALTH_WHITELIST do SecurityConfig, e o do
  # Keycloak é público por natureza. Health check autenticado deixaria todos os
  # alvos unhealthy para sempre.
  # ───────────────────────────────────────────────────────────────────────────
  node_ports = {
    api = {
      listener_port = 80
      node_port     = var.app_node_port
      health_path   = "/actuator/health/readiness"
    }
    keycloak = {
      listener_port = 81
      node_port     = var.keycloak_node_port
      # No Keycloak 24 o /health/* vive no próprio listener HTTP; a interface de
      # management na 9000 só aparece na 25. Ver infra/k8s/20-keycloak.yaml.
      health_path = "/health/ready"
    }
  }
}

resource "aws_vpc" "main" {
  cidr_block = var.vpc_cidr

  # Ambos obrigatórios para o EKS: os nós resolvem o endpoint privado do control
  # plane por DNS, e sem hostnames internos o kubelet não registra o nó.
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.project_name}-vpc"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-igw"
  }
}

# =============================================================================
# Subnets
# =============================================================================

resource "aws_subnet" "public" {
  count = length(local.azs)

  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index)
  availability_zone = local.azs[count.index]

  # O NAT Gateway precisa de IP público; sem isto ele não é criável aqui.
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.project_name}-public-${local.azs[count.index]}"
    # Tag de descoberta: um balanceador público criado por um controller do
    # Kubernetes escolheria estas subnets. Não usamos nenhum hoje, mas a tag
    # correta evita um diagnóstico difícil se alguém instalar um amanhã.
    "kubernetes.io/role/elb" = "1"
  }
}

resource "aws_subnet" "private" {
  count = length(local.azs)

  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 10)
  availability_zone = local.azs[count.index]

  tags = {
    Name                              = "${var.project_name}-private-${local.azs[count.index]}"
    "kubernetes.io/role/internal-elb" = "1"
  }
}

# =============================================================================
# NAT Gateway — saída para a internet a partir das subnets privadas.
#
# Os nós precisam de saída, mesmo sem receber conexões de fora: eles baixam a
# imagem do ECR, falam com o endpoint do EKS e enviam log para o CloudWatch.
#
# UM único NAT, não um por AZ. Um por AZ seria o correto em produção — elimina
# o custo de tráfego entre AZs e sobrevive à queda de uma zona —, mas são
# ~US$ 32/mês cada. Com um só, a queda da AZ do NAT tira a saída de internet
# dos nós da outra AZ; os pods já de pé continuam atendendo (o tráfego do API
# Gateway não passa pelo NAT), mas um pod novo não consegue baixar imagem.
# Trade-off aceito, e é o maior custo evitável desta infraestrutura.
# =============================================================================

resource "aws_eip" "nat" {
  domain = "vpc"

  tags = {
    Name = "${var.project_name}-nat-eip"
  }

  # O EIP não pode ser associado antes de existir rota para a internet.
  depends_on = [aws_internet_gateway.main]
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id

  tags = {
    Name = "${var.project_name}-nat"
  }

  depends_on = [aws_internet_gateway.main]
}

# =============================================================================
# Tabelas de rota
# =============================================================================

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${var.project_name}-rt-public"
  }
}

resource "aws_route_table_association" "public" {
  count = length(aws_subnet.public)

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Uma tabela por AZ, ainda que todas apontem para o mesmo NAT. Parece
# redundante, mas é o que permite trocar para um NAT por AZ depois mudando
# apenas o `nat_gateway_id` de cada uma — sem recriar associação de subnet.
resource "aws_route_table" "private" {
  count = length(local.azs)

  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = {
    Name = "${var.project_name}-rt-private-${local.azs[count.index]}"
  }
}

resource "aws_route_table_association" "private" {
  count = length(aws_subnet.private)

  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}

# =============================================================================
# Endpoint S3 — gateway endpoint, de graça.
#
# O ECR guarda as CAMADAS das imagens no S3. Sem este endpoint, todo download de
# imagem sai pelo NAT Gateway e é cobrado por GB processado. Com ele, o tráfego
# para o S3 da região não passa pelo NAT. Um gateway endpoint não tem custo
# horário — é economia pura.
# =============================================================================
resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.${var.aws_region}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = aws_route_table.private[*].id

  tags = {
    Name = "${var.project_name}-vpce-s3"
  }
}
