terraform {
  required_version = ">= 1.5.0"

  required_providers {
    # Provisiona o cluster kind conversando com o Docker local.
    kind = {
      source  = "tehcyx/kind"
      version = "~> 0.9"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 3.2"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.17"
    }
  }
}

# =============================================================================
# Os providers Kubernetes e Helm são configurados a partir do kubeconfig que o
# próprio kind escreve. Como esse arquivo só existe depois que o cluster é
# criado, o primeiro `apply` precisa ser feito em duas etapas:
#
#   terraform apply -target=kind_cluster.autopecas
#   terraform apply
#
# É a limitação conhecida do Terraform com providers configurados a partir de
# recursos criados no mesmo plano. Applies seguintes rodam em um passo só.
# =============================================================================

provider "kind" {}

locals {
  # Por padrão usa o kubeconfig escrito pelo kind; `kubeconfig_path_override`
  # permite apontar para outro — ver a descrição da variável.
  kubeconfig_path = var.kubeconfig_path_override != "" ? var.kubeconfig_path_override : kind_cluster.autopecas.kubeconfig_path
}

provider "kubernetes" {
  config_path = local.kubeconfig_path
}

provider "helm" {
  kubernetes {
    config_path = local.kubeconfig_path
  }
}
