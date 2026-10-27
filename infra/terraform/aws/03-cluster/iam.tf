# =============================================================================
# IAM — três papéis distintos, por razões diferentes.
#
#   cluster            → o que o control plane do EKS faz NA SUA CONTA em seu
#                        nome (criar ENIs, consultar subnets).
#   nodes              → o que cada instância EC2 pode fazer (registrar-se no
#                        cluster, baixar imagem do ECR, gerenciar IPs do CNI).
#   cluster_autoscaler → o que o POD do autoscaler pode fazer (alterar o
#                        desired_size do Auto Scaling Group).
#
# O terceiro é o que separa este cluster de um tutorial: sem ele, a alternativa
# seria dar permissão de autoscaling ao papel do NÓ — e aí QUALQUER pod no
# cluster, via metadados da instância, poderia redimensionar a frota.
# =============================================================================

# ─────────────────────────────────────────────────────────────────────────────
# Papel do control plane
# ─────────────────────────────────────────────────────────────────────────────
data "aws_iam_policy_document" "cluster_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["eks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "cluster" {
  name               = "${var.project_name}-eks-cluster"
  assume_role_policy = data.aws_iam_policy_document.cluster_assume.json
  description        = "Control plane do EKS ${local.cluster_name}"
}

resource "aws_iam_role_policy_attachment" "cluster_eks" {
  role       = aws_iam_role.cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

# ─────────────────────────────────────────────────────────────────────────────
# Papel dos nós
# ─────────────────────────────────────────────────────────────────────────────
data "aws_iam_policy_document" "nodes_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "nodes" {
  name               = "${var.project_name}-eks-nodes"
  assume_role_policy = data.aws_iam_policy_document.nodes_assume.json
  description        = "Nos do cluster EKS ${local.cluster_name}"
}

resource "aws_iam_role_policy_attachment" "nodes" {
  for_each = toset([
    # Permite ao kubelet registrar o nó e descrever recursos do cluster.
    "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy",
    # O VPC CNI atribui IPs da subnet aos pods — precisa manipular ENIs.
    "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy",
    # Puxar a imagem da aplicação do ECR. Somente leitura: o nó nunca publica.
    "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly",
    # Permite gerenciar o nó pelo SSM sem abrir porta 22 nem manter chave SSH.
    # É a razão de não existir nenhum `key_name` no launch template.
    "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
  ])

  role       = aws_iam_role.nodes.name
  policy_arn = each.value
}

# =============================================================================
# Papel do Cluster Autoscaler — via EKS Pod Identity.
#
# Pod Identity em vez de IRSA. As duas resolvem o mesmo problema (dar
# credencial AWS a um pod específico sem passar pelo papel do nó), mas o IRSA
# exige criar um OIDC provider, calcular o thumbprint do certificado do emissor
# e escrever uma trust policy com `StringEquals` na condição `sub` — três
# lugares onde um erro de digitação produz um `AccessDenied` silencioso.
#
# Pod Identity troca tudo isso por uma associação declarativa
# (namespace + service account → papel). O preço é o addon
# `eks-pod-identity-agent`, que é gratuito.
# =============================================================================
data "aws_iam_policy_document" "autoscaler_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole", "sts:TagSession"]

    principals {
      type        = "Service"
      identifiers = ["pods.eks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "cluster_autoscaler" {
  name               = "${var.project_name}-cluster-autoscaler"
  assume_role_policy = data.aws_iam_policy_document.autoscaler_assume.json
  description        = "Pod do Cluster Autoscaler no cluster ${local.cluster_name}"
}

# Permissão mínima real, não `autoscaling:*`.
#
# As ações de LEITURA não aceitam restrição por recurso (a API não as suporta),
# por isso ficam em "*". As de ESCRITA — as que efetivamente mudam a frota —
# estão restritas por condição ao ASG deste cluster: mesmo que outro cluster
# exista na mesma conta, este autoscaler não o alcança.
data "aws_iam_policy_document" "autoscaler" {
  statement {
    sid    = "LeituraDeEstado"
    effect = "Allow"
    actions = [
      "autoscaling:DescribeAutoScalingGroups",
      "autoscaling:DescribeAutoScalingInstances",
      "autoscaling:DescribeLaunchConfigurations",
      "autoscaling:DescribeScalingActivities",
      "autoscaling:DescribeTags",
      "ec2:DescribeImages",
      "ec2:DescribeInstanceTypes",
      "ec2:DescribeLaunchTemplateVersions",
      "ec2:GetInstanceTypesFromInstanceRequirements",
      "eks:DescribeNodegroup",
    ]
    resources = ["*"]
  }

  statement {
    sid    = "AlteracaoDaFrota"
    effect = "Allow"
    actions = [
      "autoscaling:SetDesiredCapacity",
      "autoscaling:TerminateInstanceInAutoScalingGroup",
      "autoscaling:UpdateAutoScalingGroup",
    ]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "autoscaling:ResourceTag/kubernetes.io/cluster/${local.cluster_name}"
      values   = ["owned"]
    }
  }
}

resource "aws_iam_role_policy" "cluster_autoscaler" {
  name   = "cluster-autoscaler"
  role   = aws_iam_role.cluster_autoscaler.id
  policy = data.aws_iam_policy_document.autoscaler.json
}

# A associação é o que liga o pod ao papel. O `service_account` precisa bater
# EXATAMENTE com o nome que o chart do autoscaler cria — fixado em
# autoscaler.tf para que os dois não possam divergir.
resource "aws_eks_pod_identity_association" "cluster_autoscaler" {
  cluster_name    = aws_eks_cluster.main.name
  namespace       = "kube-system"
  service_account = local.autoscaler_service_account
  role_arn        = aws_iam_role.cluster_autoscaler.arn

  depends_on = [aws_eks_addon.pod_identity]
}
