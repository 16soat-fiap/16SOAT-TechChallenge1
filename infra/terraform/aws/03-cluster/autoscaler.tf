# =============================================================================
# Escalabilidade em dois níveis — o requisito de "cluster com escalabilidade".
#
#   HPA               → ajusta o número de PODS conforme CPU/memória.
#                       Já existe (infra/k8s/40-app-hpa.yaml), afinado por
#                       medição. Depende do metrics-server.
#   Cluster Autoscaler → ajusta o número de NÓS quando os pods que o HPA criou
#                       não têm onde ser agendados.
#
# Um sem o outro não entrega elasticidade: só HPA esbarra no teto da frota fixa
# (pods ficam Pending), e só Cluster Autoscaler nunca é acionado, porque sem HPA
# a contagem de pods não muda e nunca falta espaço.
# =============================================================================

# ─────────────────────────────────────────────────────────────────────────────
# metrics-server — pré-requisito do HPA.
#
# Diferença em relação ao ambiente kind (infra/terraform/metrics-server.tf):
# aqui NÃO se usa `--kubelet-insecure-tls`. No kind a flag era necessária porque
# os kubelets usam certificado autoassinado; no EKS os certificados são emitidos
# pela CA do cluster e a verificação funciona. Manter a flag aqui seria
# desativar uma verificação que passaria — e aceitar um man-in-the-middle no
# caminho das métricas.
# ─────────────────────────────────────────────────────────────────────────────
resource "helm_release" "metrics_server" {
  name       = "metrics-server"
  repository = "https://kubernetes-sigs.github.io/metrics-server/"
  chart      = "metrics-server"
  version    = var.metrics_server_chart_version
  namespace  = "kube-system"

  atomic          = true
  wait            = true
  timeout         = 600
  cleanup_on_fail = true

  # Mesma janela de coleta do ambiente local: deixa o HPA reagir rápido o
  # suficiente para a reação ser visível numa demonstração.
  set {
    name  = "args[0]"
    value = "--metric-resolution=15s"
  }

  depends_on = [
    aws_eks_node_group.main,
    aws_eks_addon.coredns,
  ]
}

# ─────────────────────────────────────────────────────────────────────────────
# Cluster Autoscaler
#
# Configurado com o NOME do Auto Scaling Group, e não por auto-descoberta via
# tags. A auto-descoberta é o padrão da documentação, mas depende de o ASG
# carregar as tags `k8s.io/cluster-autoscaler/*` — que em node group gerenciado
# nem sempre aparecem. Quando faltam, o autoscaler sobe saudável e simplesmente
# não enxerga nenhum grupo para escalar: nada no log sugere o motivo.
#
# Passar o nome explícito elimina a classe inteira de problema, ao custo de o
# autoscaler não descobrir sozinho um node group novo — o que, com um único
# node group declarado em Terraform, não é uma perda.
# ─────────────────────────────────────────────────────────────────────────────
resource "helm_release" "cluster_autoscaler" {
  name       = "cluster-autoscaler"
  repository = "https://kubernetes.github.io/autoscaler"
  chart      = "cluster-autoscaler"
  version    = var.cluster_autoscaler_chart_version
  namespace  = "kube-system"

  atomic          = true
  wait            = true
  timeout         = 600
  cleanup_on_fail = true

  set {
    name  = "awsRegion"
    value = var.aws_region
  }

  set {
    name  = "autoscalingGroups[0].name"
    value = aws_eks_node_group.main.resources[0].autoscaling_groups[0].name
  }

  set {
    name  = "autoscalingGroups[0].minSize"
    value = var.node_min_size
  }

  set {
    name  = "autoscalingGroups[0].maxSize"
    value = var.node_max_size
  }

  # O nome da service account é fixado porque a associação de Pod Identity
  # (iam.tf) aponta para ele. O padrão do chart seria
  # "cluster-autoscaler-aws-cluster-autoscaler" — que não é o que está na
  # associação, e o pod ficaria sem credencial AWS.
  set {
    name  = "rbac.serviceAccount.create"
    value = "true"
  }

  set {
    name  = "rbac.serviceAccount.name"
    value = local.autoscaler_service_account
  }

  # Sem isto o autoscaler se recusa a remover um nó que tenha QUALQUER pod do
  # kube-system sem controlador próprio — e num cluster de 2 nós isso significa
  # nunca reduzir a frota. O risco aceito é uma interrupção breve de um pod de
  # sistema durante o scale-down.
  set {
    name  = "extraArgs.skip-nodes-with-system-pods"
    value = "false"
  }

  # Um nó com volume local (emptyDir) normalmente bloquearia o scale-down. A
  # API monta um emptyDir em /tmp por causa do readOnlyRootFilesystem — sem
  # isto, qualquer nó rodando a API seria considerado irremovível.
  set {
    name  = "extraArgs.skip-nodes-with-local-storage"
    value = "false"
  }

  # Descida mais rápida que o padrão (10m) para a demonstração não exigir
  # paciência. Em produção, descer devagar evita remover um nó que voltará a
  # ser necessário em minutos — cada nó novo custa ~2 min de espera.
  set {
    name  = "extraArgs.scale-down-unneeded-time"
    value = "5m"
  }

  set {
    name  = "extraArgs.scale-down-delay-after-add"
    value = "5m"
  }

  # O autoscaler é um ponto único: se ele morrer por falta de memória durante um
  # pico, a frota congela exatamente quando precisava crescer.
  set {
    name  = "resources.requests.cpu"
    value = "100m"
  }

  set {
    name  = "resources.requests.memory"
    value = "300Mi"
  }

  set {
    name  = "resources.limits.memory"
    value = "500Mi"
  }

  depends_on = [
    aws_eks_addon.coredns,
    # A credencial do pod vem daqui. Sem a associação de Pod Identity criada
    # antes, o autoscaler sobe e falha com AccessDenied na primeira varredura.
    aws_eks_pod_identity_association.cluster_autoscaler,
    # A política IAM só autoriza escrever em ASG com esta tag.
    aws_autoscaling_group_tag.cluster_owned,
  ]
}
