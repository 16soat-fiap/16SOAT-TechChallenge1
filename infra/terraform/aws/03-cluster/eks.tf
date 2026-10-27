# =============================================================================
# Cluster EKS.
#
# Sobre o endpoint do control plane ser público: ele é, e precisa ser, para que
# `kubectl` do CI e da sua máquina alcancem a API do Kubernetes. Isso NÃO
# contradiz o desenho de rede fechada — são duas superfícies diferentes:
#
#   • API do Kubernetes (porta 443 do control plane): pública, protegida por
#     autenticação IAM. Quem não tem credencial na conta não passa.
#   • Aplicação (os pods): sem nenhum caminho público. Nós em subnet privada,
#     NLB interno, e a única entrada é o API Gateway.
#
# Quem quiser fechar também a API do Kubernetes restringe `public_access_cidrs`
# ao IP de saída conhecido — mas aí o runner do GitHub Actions, cujo IP varia,
# perde o acesso.
# =============================================================================
resource "aws_eks_cluster" "main" {
  name     = local.cluster_name
  role_arn = aws_iam_role.cluster.arn
  version  = var.kubernetes_version

  vpc_config {
    # Só subnets privadas. O control plane cria ENIs nelas para falar com os
    # kubelets; não há motivo para essas ENIs estarem em subnet pública.
    subnet_ids = local.network.private_subnet_ids

    # Privado habilitado junto com o público: com os dois ligados, o tráfego dos
    # NÓS até a API resolve para o endereço privado e não sai pelo NAT Gateway.
    # Deixar só o público funcionaria, mas cobraria tráfego de NAT em cada
    # chamada de kubelet — e são muitas.
    endpoint_private_access = true
    endpoint_public_access  = true
    public_access_cidrs     = var.public_access_cidrs
  }

  access_config {
    # "API" em vez do antigo ConfigMap `aws-auth`. Editar aquele ConfigMap
    # errado era a forma clássica de perder o acesso ao próprio cluster sem
    # possibilidade de recuperação; com access entries, o mapeamento é um
    # recurso Terraform normal, versionado e reversível.
    authentication_mode = "API"

    # Dá admin a quem rodou o apply. Sem isto, o cluster nasce sem NENHUM
    # administrador — e a única correção é via API da AWS.
    bootstrap_cluster_creator_admin_permissions = true
  }

  # `api` registra as chamadas ao control plane; `authenticator`, as decisões de
  # autenticação IAM — é onde se descobre por que um `kubectl` deu 401.
  #
  # `audit` está deliberadamente de fora: é o log mais útil num incidente e
  # também o mais volumoso, e sozinho pode custar mais que o cluster num
  # ambiente de estudo.
  enabled_cluster_log_types = ["api", "authenticator"]

  # Sem isto o Terraform pode tentar criar o cluster antes de o papel ter as
  # permissões, e a AWS devolve um erro de validação pouco informativo.
  #
  # O log group entra pela mesma razão de ordem, nos dois sentidos. Na criação,
  # precisa existir antes do cluster, senão o EKS o cria primeiro (retenção
  # infinita) e o Terraform falha com "already exists". Na destruição, precisa
  # sair DEPOIS: o cluster leva ~10 min para morrer e segue escrevendo log —
  # apagado antes, o EKS o recria, e o próximo `up.sh` falha no mesmo erro.
  depends_on = [
    aws_iam_role_policy_attachment.cluster_eks,
    aws_cloudwatch_log_group.cluster,
  ]

  tags = {
    Name = local.cluster_name
  }
}

# Retenção do log do control plane. O EKS cria este grupo sozinho, com retenção
# INFINITA — e um log group que cresce para sempre é uma fatura silenciosa.
# Declará-lo aqui define a retenção antes de o cluster existir.
resource "aws_cloudwatch_log_group" "cluster" {
  name              = "/aws/eks/${local.cluster_name}/cluster"
  retention_in_days = var.log_retention_days
}

# =============================================================================
# Addons gerenciados.
#
# São componentes que todo cluster EKS precisa e que, instalados "na mão" por
# manifesto, viram dívida: atualizar exige saber qual versão combina com qual
# versão do Kubernetes. Como addon, a AWS resolve a compatibilidade.
#
# A ordem importa e está expressa pelos `depends_on`:
#   vpc-cni e kube-proxy  → precisam existir ANTES dos nós, senão o nó entra no
#                           cluster e fica NotReady por falta de rede.
#   coredns               → precisa de nó de pé para agendar seus pods.
# =============================================================================
resource "aws_eks_addon" "vpc_cni" {
  cluster_name = aws_eks_cluster.main.name
  addon_name   = "vpc-cni"

  # OVERWRITE porque o EKS pré-instala uma versão do CNI ao criar o cluster;
  # sem isto o addon falha reclamando que o recurso já existe.
  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "OVERWRITE"
}

resource "aws_eks_addon" "kube_proxy" {
  cluster_name = aws_eks_cluster.main.name
  addon_name   = "kube-proxy"

  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "OVERWRITE"
}

# O agente que entrega credencial AWS aos pods associados (ver iam.tf).
# Roda em host network, então não depende do CoreDNS.
resource "aws_eks_addon" "pod_identity" {
  cluster_name = aws_eks_cluster.main.name
  addon_name   = "eks-pod-identity-agent"

  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "OVERWRITE"

  depends_on = [aws_eks_node_group.main]
}

resource "aws_eks_addon" "coredns" {
  cluster_name = aws_eks_cluster.main.name
  addon_name   = "coredns"

  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "OVERWRITE"

  # Os pods do CoreDNS não são de host network — sem nó, ficam Pending e o
  # addon estoura o timeout.
  depends_on = [aws_eks_node_group.main]
}

# =============================================================================
# Comunicação entre o control plane e os nós.
#
# O EKS cria um "cluster security group" próprio e o anexa às ENIs do control
# plane. Quando os nós usam um SG NOSSO (ver nodes.tf, via launch template), a
# liberação automática entre os dois não acontece — e o sintoma é um nó que
# nunca sai de NotReady.
#
# Estas quatro regras são o que falta. Ficam nesta stack porque é aqui que o ID
# do cluster security group passa a existir.
# =============================================================================
resource "aws_vpc_security_group_ingress_rule" "cluster_from_nodes" {
  security_group_id            = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
  referenced_security_group_id = local.network.security_group_ids.nodes
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
  description                  = "kubelet e pods chamando a API do Kubernetes"
}

resource "aws_vpc_security_group_ingress_rule" "nodes_from_cluster_443" {
  security_group_id            = local.network.security_group_ids.nodes
  referenced_security_group_id = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
  description                  = "Control plane chamando webhooks e extension API servers"
}

# A faixa de portas efêmeras cobre `kubectl exec`, `kubectl logs` e
# `kubectl port-forward`: nos três o control plane abre conexão para a porta
# 10250 do kubelet e para portas altas. Sem esta regra, o cluster funciona
# normalmente mas `kubectl logs` trava — e a relação com security group não é
# o primeiro lugar onde se procura.
resource "aws_vpc_security_group_ingress_rule" "nodes_from_cluster_kubelet" {
  security_group_id            = local.network.security_group_ids.nodes
  referenced_security_group_id = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
  ip_protocol                  = "tcp"
  from_port                    = 1025
  to_port                      = 65535
  description                  = "kubectl exec/logs/port-forward e metrics-server"
}

resource "aws_vpc_security_group_egress_rule" "cluster_to_nodes" {
  security_group_id            = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
  referenced_security_group_id = local.network.security_group_ids.nodes
  ip_protocol                  = "-1"
  description                  = "Control plane em direcao aos nos"
}

# =============================================================================
# Acesso administrativo para papéis adicionais — tipicamente o papel que o
# GitHub Actions assume via OIDC.
#
# `bootstrap_cluster_creator_admin_permissions` cobre apenas a identidade que
# criou o cluster. Se o CI usa um papel diferente (e deve), sem uma entrada
# aqui todo `kubectl` do pipeline devolve 401.
# =============================================================================
resource "aws_eks_access_entry" "admins" {
  for_each = toset(var.cluster_admin_role_arns)

  cluster_name  = aws_eks_cluster.main.name
  principal_arn = each.value
  type          = "STANDARD"
}

resource "aws_eks_access_policy_association" "admins" {
  for_each = toset(var.cluster_admin_role_arns)

  cluster_name  = aws_eks_cluster.main.name
  principal_arn = each.value
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }

  depends_on = [aws_eks_access_entry.admins]
}
