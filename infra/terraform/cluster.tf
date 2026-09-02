# =============================================================================
# Cluster Kubernetes local (kind — Kubernetes IN Docker).
#
# Cada nó é um container Docker. Um control-plane e `worker_count` workers, para
# que o topologySpreadConstraints da API tenha onde distribuir as réplicas.
#
# Os extra_port_mappings ficam no control-plane, mas valem para todo o cluster:
# um Service NodePort escuta a mesma porta em TODOS os nós, então o tráfego que
# chega ao control-plane é roteado ao pod, esteja ele em qual nó estiver.
# =============================================================================
resource "kind_cluster" "autopecas" {
  name           = var.cluster_name
  node_image     = var.node_image
  wait_for_ready = true

  kind_config {
    kind        = "Cluster"
    api_version = "kind.x-k8s.io/v1alpha4"

    node {
      role = "control-plane"

      # Rotula o nó como ingress-ready, permitindo instalar um ingress-nginx
      # depois sem recriar o cluster.
      kubeadm_config_patches = [
        <<-PATCH
          kind: InitConfiguration
          nodeRegistration:
            kubeletExtraArgs:
              node-labels: "ingress-ready=true"
        PATCH
      ]

      # API → http://localhost:8080
      extra_port_mappings {
        container_port = 30080
        host_port      = var.app_host_port
        protocol       = "TCP"
      }

      # Keycloak → http://localhost:9080
      extra_port_mappings {
        container_port = 30081
        host_port      = var.keycloak_host_port
        protocol       = "TCP"
      }
    }

    # Um bloco `node` por worker. O provider kind não aceita `for_each` dentro
    # de blocos aninhados, então a contagem é resolvida com dynamic.
    dynamic "node" {
      for_each = range(var.worker_count)
      content {
        role = "worker"
      }
    }
  }
}

# =============================================================================
# Namespace da aplicação.
#
# Também existe em infra/k8s/00-namespace.yaml, de propósito: o Terraform
# precisa dele para criar o banco, e os manifestos precisam dele para funcionar
# num cluster que não veio deste Terraform. Aplicar os dois é idempotente — o
# segundo apenas reconcilia os labels.
# =============================================================================
resource "kubernetes_namespace" "autopecas" {
  metadata {
    name = var.namespace

    labels = {
      "app.kubernetes.io/part-of"          = "autopecas"
      "app.kubernetes.io/managed-by"       = "terraform"
      "pod-security.kubernetes.io/enforce" = "baseline"
      "pod-security.kubernetes.io/audit"   = "restricted"
      "pod-security.kubernetes.io/warn"    = "restricted"
    }
  }

  depends_on = [kind_cluster.autopecas]
}
