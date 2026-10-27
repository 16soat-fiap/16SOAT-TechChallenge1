terraform {
  # 1.10 é o piso para `use_lockfile` no backend S3 — o lock nativo que dispensa
  # a tabela DynamoDB que as versões anteriores exigiam.
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.80"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
    # Usado apenas para ler o certificado do emissor OIDC do GitHub e extrair o
    # thumbprint — ver github-oidc.tf.
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  # Sem bloco `backend`: state local, por necessidade. Ver comentário em main.tf.
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = var.project_name
      ManagedBy = "terraform"
      Stack     = "_backend"
    }
  }
}
