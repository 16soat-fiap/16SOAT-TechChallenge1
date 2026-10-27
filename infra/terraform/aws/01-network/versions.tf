terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.80"
    }
  }

  # Configuração parcial: `bucket` e `region` vêm do backend.hcl, porque um
  # bloco `backend` não aceita variáveis nem interpolação.
  #
  #   terraform init -backend-config=../backend.hcl
  #
  # `use_lockfile` é o lock nativo do S3 (Terraform >= 1.10). Substitui a tabela
  # DynamoDB que as versões anteriores exigiam para impedir dois applies
  # simultâneos sobre o mesmo state.
  backend "s3" {
    key          = "01-network/terraform.tfstate"
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
      Stack     = "01-network"
    }
  }
}
