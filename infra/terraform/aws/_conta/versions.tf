terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.80"
    }
  }

  # Sem bloco `backend`: state LOCAL. Esta stack é aplicada uma vez, pelo dono
  # da conta, antes de existir o bucket de state — e não deve depender dele,
  # porque o budget precisa sobreviver a qualquer destroy do ambiente.
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = var.project_name
      ManagedBy = "terraform"
      Stack     = "_conta"
    }
  }
}
