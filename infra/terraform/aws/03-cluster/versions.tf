terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.80"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.17"
    }
  }

  backend "s3" {
    key          = "03-cluster/terraform.tfstate"
    encrypt      = true
    use_lockfile = true
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = var.project_name
      ManagedBy = "terraform"
      Stack     = "03-cluster"
    }
  }
}

# =============================================================================
# Autenticação no cluster via `exec` — e por que não `data aws_eks_cluster_auth`.
#
# O data source `aws_eks_cluster_auth` resolve um token no momento do PLAN. Esse
# token vale 15 minutos. Num apply que cria o cluster do zero (~12 min) e só
# depois instala os charts, ele frequentemente já expirou quando o Helm precisa
# dele — e o erro que aparece é um 401 genérico, difícil de associar à causa.
#
# Com `exec`, o provider chama `aws eks get-token` no instante em que vai usar a
# credencial. Requer o AWS CLI no PATH (presente nos runners do GitHub Actions).
#
# O nome do cluster vem de um `local`, conhecido em tempo de plan — passar
# `aws_eks_cluster.main.name` aqui tornaria a configuração do provider dependente
# de um recurso ainda não criado.
# =============================================================================
provider "helm" {
  kubernetes {
    host                   = aws_eks_cluster.main.endpoint
    cluster_ca_certificate = base64decode(aws_eks_cluster.main.certificate_authority[0].data)

    exec {
      api_version = "client.authentication.k8s.io/v1beta1"
      command     = "aws"
      args = [
        "eks", "get-token",
        "--cluster-name", local.cluster_name,
        "--region", var.aws_region,
      ]
    }
  }
}
