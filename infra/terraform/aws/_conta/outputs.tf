output "operador" {
  description = "Usuário IAM que roda o Terraform no lugar do root."
  value       = aws_iam_user.operador.name
}

output "budget" {
  description = "Nome do budget mensal da conta."
  value       = aws_budgets_budget.mensal.name
}

output "proximos_passos" {
  description = "O que falta fazer à mão, no console, como root."
  value       = <<-EOT

    1. IAM > Users > ${aws_iam_user.operador.name} > Security credentials:
         • Enable console access, senha autogerada, "must create new password".
         • Assign MFA device (app autenticador).
       NÃO crie access key: o terminal usa `aws login`.

    2. Confirme a inscrição do e-mail do budget, se a AWS pedir.

    3. No terminal, troque o profile para o operador:

         aws logout --profile ${var.project_name}
         aws login  --profile ${var.project_name}
         (no browser, entre como IAM user: conta ${local.conta}, usuário ${aws_iam_user.operador.name})
         aws sts get-caller-identity --profile ${var.project_name}   # deve mostrar :user/, não :root

  EOT
}
