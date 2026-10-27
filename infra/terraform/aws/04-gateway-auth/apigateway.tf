# =============================================================================
# API Gateway (HTTP API) — a única porta de entrada do sistema.
#
# HTTP API e não REST API: para este uso (proxy com authorizer Lambda e
# integração privada) entrega o mesmo resultado por ~1/3 do preço e com menos
# configuração. O que se perde do REST API — planos de uso com chave de API,
# cache de resposta, transformações de corpo por template — não é usado aqui.
#
# "Controle e roteamento", concretamente:
#
#   roteamento → três destinos distintos a partir do caminho: /api/* vai para a
#                aplicação, /realms/* vai para o Keycloak, e /health é reescrito
#                para /actuator/health antes de seguir.
#   controle   → authorizer na borda (quem não tem token válido nunca alcança o
#                cluster), throttling por stage, e log de acesso estruturado com
#                a identidade de quem chamou.
# =============================================================================
resource "aws_apigatewayv2_api" "main" {
  name          = "${var.project_name}-api"
  description   = "Gateway do ${var.project_name} — Tech Challenge Fase 3"
  protocol_type = "HTTP"

  # O preflight é respondido pelo PRÓPRIO gateway, sem chegar à aplicação. Sem
  # isto, cada requisição de um frontend viraria duas no cluster — e o OPTIONS
  # não carrega Authorization, então seria recusado pelo authorizer.
  cors_configuration {
    allow_origins = var.cors_allow_origins
    allow_methods = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
    allow_headers = ["authorization", "content-type"]
    max_age       = 300
  }
}

# =============================================================================
# VPC Link — como o gateway, que é um serviço gerenciado fora da nossa VPC,
# alcança um balanceador que não tem endereço público.
#
# A AWS cria ENIs nas subnets informadas; essas ENIs usam o security group
# `sg_vpclink` da stack 01, cuja única regra é "sair para o NLB".
#
# É o elo que permite o cluster não ter nenhuma entrada pública.
# =============================================================================
resource "aws_apigatewayv2_vpc_link" "main" {
  name               = "${var.project_name}-vpclink"
  subnet_ids         = local.network.private_subnet_ids
  security_group_ids = [local.network.security_group_ids.vpclink]

  tags = {
    Name = "${var.project_name}-vpclink"
  }
}

# =============================================================================
# Integrações — para onde cada rota manda o tráfego.
#
# `integration_uri` é o ARN de um LISTENER do NLB, não do balanceador: é assim
# que o gateway sabe em qual porta entregar, e é por isso que a stack 03 exporta
# `listener_arns` em vez do ARN do NLB.
#
# `payload_format_version = "1.0"` é o único valor aceito para HTTP_PROXY — o
# 2.0 existe apenas para integração com Lambda.
# =============================================================================
resource "aws_apigatewayv2_integration" "api" {
  api_id           = aws_apigatewayv2_api.main.id
  integration_type = "HTTP_PROXY"
  # ANY: o método da requisição original é repassado. Fixar POST aqui faria todo
  # GET chegar à aplicação como POST.
  integration_method     = "ANY"
  integration_uri        = local.cluster.listener_arns["api"]
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.main.id
  payload_format_version = "1.0"

  # 29s é o teto do API Gateway. Deixar no máximo transfere a decisão de
  # timeout para a aplicação, que sabe quais operações são lentas; um teto menor
  # aqui cortaria uma resposta já em elaboração.
  timeout_milliseconds = 29000
}

# Mesma integração, com o caminho REESCRITO.
#
# Existe para que o health check público seja `GET /health` em vez de
# `/actuator/health` — o cliente não precisa saber que o serviço por trás é
# Spring Boot, e o dia em que deixar de ser, a URL não muda. É também a
# demonstração mais direta de transformação no gateway.
resource "aws_apigatewayv2_integration" "health" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "GET"
  integration_uri        = local.cluster.listener_arns["api"]
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.main.id
  payload_format_version = "1.0"

  request_parameters = {
    "overwrite:path" = "/actuator/health"
  }

  timeout_milliseconds = 10000
}

resource "aws_apigatewayv2_integration" "keycloak" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = local.cluster.listener_arns["keycloak"]
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.main.id
  payload_format_version = "1.0"

  timeout_milliseconds = 29000
}

# =============================================================================
# Authorizer.
#
# `enable_simple_responses` deixa a Lambda devolver `{ isAuthorized: bool }` em
# vez de montar um documento de política IAM. A política granular faz sentido
# quando o gateway decide permissão por rota; aqui a decisão é binária — entra
# ou não entra — e a permissão fina é da aplicação.
#
# Sobre o cache de 300s: a mesma combinação de token é reavaliada no máximo uma
# vez a cada 5 minutos, o que derruba a latência e o custo em tráfego repetido.
# A contrapartida é que um token revogado continua passando pela borda até o
# cache vencer — aceitável porque revogação não é verificada em lugar nenhum
# deste sistema (não há introspecção), e o `exp` curto do Keycloak é o
# mecanismo real de expiração.
# =============================================================================
resource "aws_apigatewayv2_authorizer" "keycloak" {
  api_id = aws_apigatewayv2_api.main.id
  name   = "${var.project_name}-keycloak-jwt"

  authorizer_type                   = "REQUEST"
  authorizer_uri                    = aws_lambda_function.authorizer.invoke_arn
  authorizer_payload_format_version = "2.0"
  enable_simple_responses           = true
  authorizer_result_ttl_in_seconds  = var.authorizer_cache_ttl_seconds

  # Duas funções: é o que a Lambda lê em `identitySource[0]`, e é a CHAVE do
  # cache. Com o header Authorization como chave, dois usuários diferentes nunca
  # compartilham uma decisão em cache.
  identity_sources = ["$request.header.Authorization"]
}

# =============================================================================
# Rotas.
#
# A separação entre protegido e público é a parte que merece leitura:
#
#   /api/*      → authorizer. É a aplicação.
#   /realms/*   → SEM authorizer. É o Keycloak, onde se OBTÉM o token; exigir
#                 token aqui seria exigir um token para pedir um token.
#   /health     → SEM authorizer. Health check precisa funcionar sem credencial.
#   swagger     → SEM authorizer, e opcional. A documentação é pública; os
#                 endpoints que ela descreve, não.
# =============================================================================
resource "aws_apigatewayv2_route" "api" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "ANY /api/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.api.id}"

  authorization_type = "CUSTOM"
  authorizer_id      = aws_apigatewayv2_authorizer.keycloak.id
}

resource "aws_apigatewayv2_route" "keycloak" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "ANY /realms/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.keycloak.id}"

  authorization_type = "NONE"
}

resource "aws_apigatewayv2_route" "health" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /health"
  target    = "integrations/${aws_apigatewayv2_integration.health.id}"

  authorization_type = "NONE"
}

locals {
  # Swagger UI pede estes quatro caminhos. As duas variantes de cada um
  # (exato e {proxy+}) são necessárias: `{proxy+}` NÃO casa com o caminho vazio,
  # então sem a rota exata um GET /v3/api-docs devolve 404.
  rotas_documentacao = var.expose_swagger ? toset([
    "GET /swagger-ui.html",
    "GET /swagger-ui/{proxy+}",
    "GET /v3/api-docs",
    "GET /v3/api-docs/{proxy+}",
  ]) : toset([])
}

resource "aws_apigatewayv2_route" "documentacao" {
  for_each = local.rotas_documentacao

  api_id    = aws_apigatewayv2_api.main.id
  route_key = each.value
  target    = "integrations/${aws_apigatewayv2_integration.api.id}"

  authorization_type = "NONE"
}

# =============================================================================
# Stage.
#
# `$default` é o stage sem prefixo de caminho na URL. Importa mais do que
# parece: um stage nomeado faria toda URL ser
# `https://.../prod/api/clientes`, e o Keycloak montaria o `iss` sem esse
# prefixo — quebrando a validação de forma difícil de enxergar.
# =============================================================================
resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.main.id
  name        = "$default"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.gateway.arn

    # Log em JSON, não em texto: é o que permite consultar com CloudWatch Logs
    # Insights sem escrever expressão regular.
    #
    # `authorizer.usuario` vem do `context` devolvido pela Lambda — é o que
    # torna cada requisição rastreável até uma pessoa sem cruzar com o log da
    # aplicação. Em rota sem authorizer o campo vem vazio.
    format = jsonencode({
      requestId       = "$context.requestId"
      requestTime     = "$context.requestTime"
      httpMethod      = "$context.httpMethod"
      path            = "$context.path"
      routeKey        = "$context.routeKey"
      status          = "$context.status"
      responseLatency = "$context.responseLatency"
      sourceIp        = "$context.identity.sourceIp"
      usuario         = "$context.authorizer.usuario"
      roles           = "$context.authorizer.roles"
      # Latência e status da integração separados do total: é como se distingue
      # "a aplicação está lenta" de "o gateway está devolvendo erro".
      integrationStatus  = "$context.integration.status"
      integrationLatency = "$context.integration.latency"
      integrationError   = "$context.integrationErrorMessage"
    })
  }

  default_route_settings {
    # Throttling no stage é a proteção mais barata que existe aqui: sem ele, uma
    # rajada — um teste de carga mal calibrado, um cliente em laço — chega
    # inteira ao cluster e o HPA responde escalando, ou seja, gastando.
    throttling_rate_limit  = var.throttling_rate_limit
    throttling_burst_limit = var.throttling_burst_limit

    # Métricas por rota no CloudWatch. Custa alguns centavos e é a diferença
    # entre ver "a API está lenta" e ver QUAL rota está lenta.
    detailed_metrics_enabled = true
  }
}

resource "aws_cloudwatch_log_group" "gateway" {
  name              = "/aws/apigateway/${var.project_name}"
  retention_in_days = var.log_retention_days
}
