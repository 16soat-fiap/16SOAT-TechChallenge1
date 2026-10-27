output "api_base_url" {
  description = "URL pública do sistema. A única porta de entrada."
  value       = local.api_base_url
}

output "issuer" {
  description = "Claim `iss` dos tokens. O mesmo valor na API, na Lambda e no Keycloak."
  value       = local.issuer_publico
}

output "token_endpoint" {
  description = "Onde pedir um token (grant password ou client_credentials)."
  value       = "${local.issuer_publico}/protocol/openid-connect/token"
}

output "health_url" {
  description = "Health check público — o gateway reescreve para /actuator/health."
  value       = "${local.api_base_url}/health"
}

output "swagger_url" {
  description = "Swagger UI, quando `expose_swagger` está ligado."
  value       = var.expose_swagger ? "${local.api_base_url}/swagger-ui.html" : null
}

output "keycloak_admin_console_acesso" {
  description = <<-EOT
    Como alcançar o console administrativo do Keycloak.

    Ele NÃO é publicado no API Gateway, de propósito: as rotas do gateway são
    apenas /realms/*, /health, /api/* e a documentação. Expor o console de
    administração do emissor de identidade na internet não se justifica — e as
    rotas que ele precisa (/admin/*, /resources/*, /js/*) teriam que ser abertas
    sem authorizer, porque a autenticação dele é por sessão, não por Bearer.

    O acesso é por túnel, autenticado pelo IAM da AWS antes de chegar ao
    Keycloak.
  EOT
  value       = <<-EOT

    kubectl port-forward -n ${var.namespace} svc/keycloak 8080:8080
    # → http://localhost:8080/admin  (usuário: ${var.keycloak_admin_user})
    #   senha: terraform -chdir=04-gateway-auth output -raw keycloak_admin_password

  EOT
}

output "keycloak_admin_password" {
  description = "Senha do console administrativo, gerada pelo Terraform."
  value       = random_password.keycloak_admin.result
  sensitive   = true
}

output "authorizer_function_name" {
  description = "Nome da Lambda. Para ver o log: aws logs tail /aws/lambda/<nome> --follow"
  value       = aws_lambda_function.authorizer.function_name
}

output "como_testar" {
  description = "Roteiro de verificação depois do apply."
  value       = <<-EOT

    # 1 ─ Health check, sem token (deve responder 200)
    curl -i ${local.api_base_url}/health

    # 2 ─ Endpoint protegido SEM token (deve responder 403, barrado na borda —
    #     o tráfego nem chega ao cluster)
    curl -i ${local.api_base_url}/api/clientes

    # 3 ─ Obter um token no Keycloak
    #     O client ${var.keycloak_client_id} é public e tem directAccessGrants,
    #     então o grant password dispensa client_secret. O realm exportado traz
    #     um único usuário, `test_user`, com a role ADMIN.
    TOKEN=$(curl -s -X POST \
      ${local.issuer_publico}/protocol/openid-connect/token \
      -d grant_type=password \
      -d client_id=${var.keycloak_client_id} \
      -d username=test_user \
      -d password=<senha definida no realm-export.json> | jq -r .access_token)

    # 3b ─ Conferir que o `iss` do token bate com o esperado pela Lambda e pela
    #      API. Divergência aqui é a causa mais provável de 403 com token válido.
    echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq '{iss, exp, email, realm_access}'
    # esperado → "iss": "${local.issuer_publico}"

    # 4 ─ Mesma chamada COM token (deve responder 200)
    curl -i ${local.api_base_url}/api/clientes -H "Authorization: Bearer $TOKEN"

    # 5 ─ Token adulterado (deve responder 403 e registrar o motivo no log da Lambda)
    curl -i ${local.api_base_url}/api/clientes -H "Authorization: Bearer $TOKEN-adulterado"
    aws logs tail /aws/lambda/${aws_lambda_function.authorizer.function_name} --since 2m

  EOT
}
