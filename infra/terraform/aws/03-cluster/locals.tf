data "terraform_remote_state" "network" {
  backend = "s3"

  config = {
    bucket = var.state_bucket
    key    = "01-network/terraform.tfstate"
    region = var.aws_region
  }
}

locals {
  network      = data.terraform_remote_state.network.outputs
  cluster_name = local.network.cluster_name

  # Nome da service account do Cluster Autoscaler, fixado num único lugar.
  #
  # Ele aparece em dois lugares que PRECISAM concordar: a associação de Pod
  # Identity (iam.tf) e o chart do Helm (autoscaler.tf). Se divergirem, o pod
  # sobe normalmente e falha só na primeira tentativa de escalar, com um
  # AccessDenied no log do autoscaler — sintoma caro de diagnosticar.
  autoscaler_service_account = "cluster-autoscaler"
}
