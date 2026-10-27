# =============================================================================
# Configuração da aplicação e do Keycloak dentro do cluster.
#
# Por que esta stack, e não a 03: o ConfigMap carrega a URL pública do API
# Gateway, que nasce aqui. Com ele na stack 03, as duas stacks dependeriam uma
# da outra. A leitura que justifica o arranjo está no README da raiz — o
# ConfigMap É o contrato entre a camada de autenticação e a aplicação, e quem
# define o contrato é quem emite o token.
#
# O que NÃO está aqui: Deployment, Service, HPA, PDB. Esses continuam em
# infra/k8s/aws/ como manifestos aplicados por `kubectl`, porque mudam a cada
# deploy de imagem e não têm por que passar por um ciclo de Terraform.
# =============================================================================

# Também declarado em infra/k8s/aws/00-namespace.yaml, de propósito — o mesmo
# arranjo da Fase 2 (ver infra/terraform/cluster.tf): o Terraform precisa do
# namespace para criar os Secrets, e os manifestos precisam dele para funcionar
# num cluster que não veio deste Terraform. Aplicar os dois é idempotente.
resource "kubernetes_namespace" "autopecas" {
  metadata {
    name = var.namespace

    labels = {
      "app.kubernetes.io/part-of"          = var.project_name
      "app.kubernetes.io/managed-by"       = "terraform"
      "pod-security.kubernetes.io/enforce" = "baseline"
      "pod-security.kubernetes.io/audit"   = "restricted"
      "pod-security.kubernetes.io/warn"    = "restricted"
    }
  }
}

# =============================================================================
# Aplicação
# =============================================================================
resource "kubernetes_config_map" "app" {
  metadata {
    name      = "autopecas-config"
    namespace = kubernetes_namespace.autopecas.metadata[0].name

    labels = {
      "app.kubernetes.io/name"       = "autopecas-api"
      "app.kubernetes.io/part-of"    = var.project_name
      "app.kubernetes.io/managed-by" = "terraform"
    }
  }

  data = {
    SPRING_PROFILES_ACTIVE = "prod"

    # Já vem com `?sslmode=require` — o parameter group do RDS exige TLS.
    DB_URL = local.database.jdbc_url

    # ─────────────────────────────────────────────────────────────────────────
    # A assimetria entre issuer e JWKS é deliberada, e o application-prod.yml
    # já previa as duas variáveis separadas.
    #
    #   ISSUER é uma IDENTIDADE: tem que ser byte a byte igual ao `iss` que o
    #   Keycloak escreve no token — e ele escreve o endereço público, porque é
    #   por ali que o cliente pede o token.
    #
    #   JWK_URI é um ENDEREÇO para buscar: a aplicação está dentro do cluster e
    #   alcança o Keycloak pelo Service, sem sair para a internet e sem depender
    #   de o caminho público estar de pé.
    # ─────────────────────────────────────────────────────────────────────────
    OAUTH2_ISSUER_URI = local.issuer_publico
    OAUTH2_JWK_URI    = local.jwks_uri_interno

    KEYCLOAK_AUTH_SERVER_URL = local.api_base_url
    KEYCLOAK_REALM           = var.keycloak_realm
    KEYCLOAK_CLIENT_ID       = var.keycloak_client_id

    TZ = "UTC"
  }
}

resource "kubernetes_secret" "app" {
  metadata {
    name      = "autopecas-secrets"
    namespace = kubernetes_namespace.autopecas.metadata[0].name

    labels = {
      "app.kubernetes.io/name"       = "autopecas-api"
      "app.kubernetes.io/part-of"    = var.project_name
      "app.kubernetes.io/managed-by" = "terraform"
    }
  }

  type = "Opaque"

  # Diferença concreta em relação ao infra/k8s/11-secret-app.yaml da Fase 2,
  # que traz "postgres/postgres" versionado: aqui o valor vem do Secrets
  # Manager, gerado e rotacionado pelo RDS, e nenhuma senha existe no
  # repositório. Ela existe no state desta stack — ver o comentário em
  # locals.tf sobre o External Secrets Operator como próximo passo.
  data = {
    DB_USERNAME = local.db_credenciais.username
    DB_PASSWORD = local.db_credenciais.password
  }
}

# =============================================================================
# Keycloak
# =============================================================================

resource "kubernetes_config_map" "keycloak" {
  metadata {
    name      = "keycloak-config"
    namespace = kubernetes_namespace.autopecas.metadata[0].name

    labels = {
      "app.kubernetes.io/name"       = "keycloak"
      "app.kubernetes.io/part-of"    = var.project_name
      "app.kubernetes.io/managed-by" = "terraform"
    }
  }

  data = {
    KC_DB = "postgres"

    # ─────────────────────────────────────────────────────────────────────────
    # O MESMO banco da aplicação, em SCHEMA separado.
    #
    # Na Fase 2 eram dois bancos (`app_db` e `keycloak_db`) no mesmo Postgres.
    # No RDS, criar um segundo banco exigiria rodar `CREATE DATABASE` de dentro
    # da VPC — o Terraform, rodando de fora, não alcança a instância.
    #
    # Um schema resolve igual de bem e o motivo original se mantém: as dezenas
    # de tabelas do Keycloak ficam fora do `public`, onde o `ddl-auto: validate`
    # do Hibernate e o Flyway trabalham. Nenhum dos dois enxerga o outro.
    #
    # O schema é criado por um initContainer no Deployment do Keycloak
    # (infra/k8s/aws/20-keycloak.yaml) — o Keycloak não cria o schema sozinho.
    # ─────────────────────────────────────────────────────────────────────────
    KC_DB_URL    = local.database.jdbc_url
    KC_DB_SCHEMA = var.keycloak_schema

    # As partes soltas do endereço do banco, para o initContainer que cria o
    # schema. Ele usa `psql`, que lê PGHOST/PGPORT/PGDATABASE — e não entende
    # uma URL JDBC.
    DB_HOST = local.database.db_address
    DB_PORT = tostring(local.database.db_port)
    DB_NAME = local.database.db_name

    # O endereço público que entra no claim `iss`. No Keycloak 24 a variável é
    # KC_HOSTNAME_URL e recebe a URL completa; a partir da 25 isso virou
    # KC_HOSTNAME. Vale conferir ao atualizar a imagem.
    KC_HOSTNAME_URL = local.api_base_url

    # KC_HOSTNAME_ADMIN_URL deliberadamente NÃO definido.
    #
    # Apontá-lo para o API Gateway faria o console administrativo gerar links
    # para /admin/*, /resources/* e /js/* — caminhos que NÃO têm rota no
    # gateway, porque o console não é publicado na internet. O resultado seria
    # um console que carrega pela metade.
    #
    # Sem a variável, o console monta os links a partir do Host da requisição,
    # que é exatamente o que se quer no acesso por port-forward:
    #
    #   kubectl port-forward -n autopecas svc/keycloak 8080:8080
    #   # → http://localhost:8080/admin
    KC_HOSTNAME_STRICT = "false"
    KC_HTTP_ENABLED    = "true"
    KC_HEALTH_ENABLED  = "true"

    # O TLS termina no API Gateway. Sem isto o Keycloak vê a conexão como HTTP
    # simples e monta redirecionamentos com `http://`, que o navegador recusa
    # como conteúdo misto.
    KC_PROXY_HEADERS = "xforwarded"
  }
}

# Senha do console administrativo.
#
# Gerada pelo Terraform em vez de fixada: um "admin/admin" versionado num
# Keycloak alcançável pela internet é um problema de verdade, não de estilo.
# O valor fica no state e sai pelo output (sensível) — para lê-lo:
#   terraform -chdir=04-gateway-auth output -raw keycloak_admin_password
resource "random_password" "keycloak_admin" {
  length = 24
  # Sem símbolos: a senha é digitada no console e usada em scripts, e caractere
  # especial em shell é fonte de erro bobo. 24 caracteres alfanuméricos já dão
  # ~140 bits de entropia.
  special = false
}

resource "kubernetes_secret" "keycloak" {
  metadata {
    name      = "keycloak-secrets"
    namespace = kubernetes_namespace.autopecas.metadata[0].name

    labels = {
      "app.kubernetes.io/name"       = "keycloak"
      "app.kubernetes.io/part-of"    = var.project_name
      "app.kubernetes.io/managed-by" = "terraform"
    }
  }

  type = "Opaque"

  data = {
    KEYCLOAK_ADMIN          = var.keycloak_admin_user
    KEYCLOAK_ADMIN_PASSWORD = random_password.keycloak_admin.result

    # O Keycloak usa o MESMO usuário do banco da aplicação. Separá-los seria
    # melhor — cada um com permissão apenas no seu schema —, mas criar um
    # segundo usuário exige `CREATE ROLE` de dentro da VPC, a mesma limitação
    # que levou ao schema compartilhado.
    KC_DB_USERNAME = local.db_credenciais.username
    KC_DB_PASSWORD = local.db_credenciais.password
  }
}

# O realm exportado, igual à Fase 2: um único arquivo serve ao Compose, ao kind
# e ao EKS, então não há como as três configurações divergirem.
resource "kubernetes_config_map" "keycloak_realm" {
  metadata {
    name      = "keycloak-realm"
    namespace = kubernetes_namespace.autopecas.metadata[0].name

    labels = {
      "app.kubernetes.io/name"       = "keycloak"
      "app.kubernetes.io/part-of"    = var.project_name
      "app.kubernetes.io/managed-by" = "terraform"
    }
  }

  data = {
    "realm-export.json" = file("${path.module}/../../../../keycloak/realm-export.json")
  }
}
