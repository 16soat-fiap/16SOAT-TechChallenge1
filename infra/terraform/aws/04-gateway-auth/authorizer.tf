# =============================================================================
# Lambda Authorizer — a function serverless de autenticação.
#
# Código em infra/lambda/authorizer/. Sem dependências: apenas `node:crypto`,
# então o pacote de deploy são três arquivos .mjs e nada mais — sem
# node_modules, sem bundler, sem passo de build.
#
# O que ela decide: se a requisição entra na VPC. O que ela NÃO decide: o que o
# usuário pode fazer lá dentro. Autorização por role continua sendo da
# aplicação (@PreAuthorize), onde a regra fica junto da operação que ela
# protege.
# =============================================================================

data "archive_file" "authorizer" {
  type        = "zip"
  source_dir  = "${path.module}/../../../lambda/authorizer"
  output_path = "${path.module}/.build/authorizer.zip"

  # O arquivo de teste não vai para produção: aumentaria o pacote e, com ele, o
  # cold start.
  excludes = ["jwt.test.mjs", "package.json"]
}

resource "aws_lambda_function" "authorizer" {
  function_name = "${var.project_name}-authorizer"
  description   = "Valida o JWT emitido pelo Keycloak na borda do API Gateway"

  role    = aws_iam_role.authorizer.arn
  handler = "index.handler"
  runtime = var.lambda_runtime

  filename = data.archive_file.authorizer.output_path
  # Sem isto o Terraform não percebe mudança no CÓDIGO — só no nome do arquivo
  # zip, que não muda. O deploy de uma correção passaria silenciosamente.
  source_code_hash = data.archive_file.authorizer.output_base64sha256

  # 256 MB: a função só faz uma verificação RSA e um parse de JSON. Na Lambda a
  # CPU é proporcional à memória, então subir ajuda o cold start — mas de 128
  # para 256 a diferença já está quase toda capturada.
  memory_size = 256

  # 5s é folgado para o caminho normal (cache em memória, nenhuma chamada de
  # rede). O pior caso é o cold start com busca do JWKS, e o cliente HTTP já
  # tem timeout de 3s.
  timeout = 5

  # x86_64 e não arm64: o pacote é JavaScript puro, as duas arquiteturas
  # serviriam, e arm64 seria ~20% mais barato. Fica em x86_64 porque o volume
  # desta função está inteiro no nível gratuito — a economia seria sobre zero.
  architectures = ["x86_64"]

  environment {
    variables = {
      JWKS_URL = local.jwks_uri_lambda
      ISSUER   = local.issuer_publico

      # Usado apenas para extrair resource_access.<client>.roles para o log.
      CLIENT_ID = var.keycloak_client_id

      # Vazio de propósito: o access token do Keycloak traz `aud: "account"`, e
      # o resource server do Spring também não valida audiência. Exigi-la aqui
      # recusaria todo token emitido pelo realm atual. Ver o comentário em
      # validarClaims (jwt.mjs).
      AUDIENCES = join(",", var.jwt_audiences)

      JWKS_TTL_SECONDS   = tostring(var.jwks_cache_ttl_seconds)
      CLOCK_SKEW_SECONDS = tostring(var.clock_skew_seconds)
    }
  }

  # A função é chamada em TODA requisição a /api/*: tracing ativo é o que
  # permite ver quanto da latência total é dela.
  tracing_config {
    mode = "Active"
  }

  depends_on = [
    aws_iam_role_policy_attachment.authorizer_logs,
    # Sem o log group criado antes, a Lambda cria um sozinha com retenção
    # infinita — e aí a configuração de retenção abaixo nunca se aplica.
    aws_cloudwatch_log_group.authorizer,
  ]
}

resource "aws_cloudwatch_log_group" "authorizer" {
  name              = "/aws/lambda/${var.project_name}-authorizer"
  retention_in_days = var.log_retention_days
}

# =============================================================================
# Papel da Lambda.
#
# Ela não acessa NENHUM recurso da AWS — nem banco, nem S3, nem Secrets
# Manager. Só escreve log e envia trace. É o que torna a permissão trivial, e é
# consequência direta do desenho: a validação de um JWT precisa apenas da chave
# pública, que vem por HTTP.
# =============================================================================
data "aws_iam_policy_document" "authorizer_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "authorizer" {
  name               = "${var.project_name}-authorizer"
  assume_role_policy = data.aws_iam_policy_document.authorizer_assume.json
  description        = "Lambda authorizer do API Gateway"
}

resource "aws_iam_role_policy_attachment" "authorizer_logs" {
  role       = aws_iam_role.authorizer.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "authorizer_tracing" {
  role       = aws_iam_role.authorizer.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

# =============================================================================
# Permissão para o API Gateway invocar a função.
#
# `source_arn` aponta para ESTE authorizer especificamente, e não para a API
# inteira. Sem a restrição, qualquer API Gateway da conta poderia invocar a
# função — inofensivo hoje, mas é a diferença entre uma permissão e uma
# permissão com escopo.
# =============================================================================
resource "aws_lambda_permission" "apigateway" {
  statement_id  = "AllowInvokeFromApiGatewayAuthorizer"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.authorizer.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.main.execution_arn}/authorizers/${aws_apigatewayv2_authorizer.keycloak.id}"
}
