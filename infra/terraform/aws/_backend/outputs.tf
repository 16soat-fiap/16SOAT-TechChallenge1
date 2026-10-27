output "state_bucket" {
  description = "Nome do bucket de state. Vai no backend.hcl das outras stacks."
  value       = aws_s3_bucket.state.id
}

output "ecr_repository_url" {
  description = "URL do ECR para `docker push`. Vai no secret ECR_REPOSITORY do GitHub."
  value       = aws_ecr_repository.app.repository_url
}

output "github_deploy_role_arn" {
  description = <<-EOT
    Papel que o GitHub Actions assume via OIDC, ou null se
    `enable_github_oidc = false`.

    Quando existe, vai em DOIS lugares, e os dois são necessários:
      • secret AWS_DEPLOY_ROLE_ARN do repositório (para o workflow assumi-lo);
      • `cluster_admin_role_arns` do terraform.tfvars (para o EKS autorizá-lo).

    Sem ele, o deploy é feito por infra/scripts/deploy.sh, com a credencial de
    quem está no terminal.
  EOT
  value       = one(aws_iam_role.github_deploy[*].arn)
}

output "proximos_passos" {
  description = <<-EOT
    O que fazer com os valores acima.

    Quem usa infra/scripts/up.sh não precisa deste roteiro — o script escreve o
    backend.hcl e o terraform.tfvars a partir destes outputs e segue com as
    quatro stacks. Isto aqui é o passo a passo manual equivalente.
  EOT
  value       = <<-EOT

    1. infra/terraform/aws/backend.hcl:

         bucket = "${aws_s3_bucket.state.id}"
         region = "${var.aws_region}"

    2. infra/terraform/aws/terraform.tfvars:

         state_bucket = "${aws_s3_bucket.state.id}"
         aws_region   = "${var.aws_region}"
    ${var.enable_github_oidc ? format("     cluster_admin_role_arns = [\"%s\"]", one(aws_iam_role.github_deploy[*].arn)) : "     # cluster_admin_role_arns: nada a pôr, OIDC desabilitado"}

    3. ${var.enable_github_oidc ? "Secrets no repositório do GitHub:" : "CD pelo GitHub Actions: desabilitado (enable_github_oidc = false)."}
    ${var.enable_github_oidc ? format("     AWS_DEPLOY_ROLE_ARN = %s\n         ECR_REPOSITORY      = %s", one(aws_iam_role.github_deploy[*].arn), aws_ecr_repository.app.repository_url) : format("     Para fazer deploy sem pipeline:  infra/scripts/deploy.sh\n         ECR deste ambiente:              %s", aws_ecr_repository.app.repository_url)}

    4. Stacks seguintes:

         terraform -chdir=01-network init -backend-config=../backend.hcl
         terraform -chdir=01-network apply -var-file=../terraform.tfvars

  EOT
}
