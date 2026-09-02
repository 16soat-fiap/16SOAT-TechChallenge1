# =============================================================================
# metrics-server — pré-requisito do HPA.
#
# O HorizontalPodAutoscaler lê a Metrics API (metrics.k8s.io); sem alguém
# servindo essa API, o HPA fica com TARGETS em <unknown> e nunca escala. O kind
# não traz o metrics-server, então ele é instalado aqui.
#
# --kubelet-insecure-tls é necessário no kind: os kubelets usam certificados
# autoassinados que a CA do cluster não assina, e sem a flag o metrics-server
# rejeita a conexão e não coleta nada. É aceitável em cluster local; em cloud
# gerenciada os certificados já vêm assinados e a flag não deve ser usada.
# =============================================================================
resource "helm_release" "metrics_server" {
  count = var.install_metrics_server ? 1 : 0

  name       = "metrics-server"
  repository = "https://kubernetes-sigs.github.io/metrics-server/"
  chart      = "metrics-server"
  version    = "3.12.2"
  namespace  = "kube-system"

  # O HPA só é útil depois que o cluster responde; sem isso o Helm pode tentar
  # instalar antes do control-plane estar pronto.
  atomic          = true
  wait            = true
  timeout         = 300
  cleanup_on_fail = true

  set {
    name  = "args[0]"
    value = "--kubelet-insecure-tls"
  }

  # Prefere o IP interno do nó. No kind, resolver o hostname do nó a partir do
  # pod nem sempre funciona.
  set {
    name  = "args[1]"
    value = "--kubelet-preferred-address-types=InternalIP\\,Hostname\\,ExternalIP"
  }

  # Janela de coleta menor deixa o HPA reagir mais rápido em demonstração.
  set {
    name  = "args[2]"
    value = "--metric-resolution=15s"
  }

  depends_on = [kind_cluster.autopecas]
}
