# =============================================================================
# Bootstrap — o que precisa existir ANTES das outras stacks.
#
# Esta stack usa state LOCAL por necessidade lógica: ela cria o bucket onde as
# demais guardam o state, e não poderia guardar o próprio state num bucket que
# ainda não existe.
#
# Perder este state não é grave — o bucket e o ECR continuam de pé, e um
# `terraform import` os traz de volta. Por isso ele fica fora do git.
# =============================================================================

# Sufixo aleatório no nome do bucket: nome de bucket S3 é global entre TODAS as
# contas AWS do mundo, então "autopecas-tfstate" quase certamente já existe.
resource "random_id" "sufixo" {
  byte_length = 4
}

resource "aws_s3_bucket" "state" {
  bucket = "${var.project_name}-tfstate-${random_id.sufixo.hex}"

  # Impede que um `terraform destroy` daqui apague o state de todas as outras
  # stacks — o erro mais caro possível nesta pasta.
  lifecycle {
    prevent_destroy = true
  }
}

# Versionamento é o que torna o state recuperável. Um apply interrompido no
# momento errado pode gravar um state parcial; com versionamento a versão
# anterior continua lá.
resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.state.id

  versioning_configuration {
    status = "Enabled"
  }
}

# O state guarda, em texto claro, tudo que foi provisionado — incluindo a senha
# do RDS e o conteúdo dos Secrets do Kubernetes. A criptografia em repouso é o
# mínimo; o controle de acesso ao bucket é o que realmente protege.
resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.state.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket = aws_s3_bucket.state.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Expira versões antigas do state. Sem isto o bucket cresce para sempre: cada
# apply grava uma versão nova de cada arquivo de state.
resource "aws_s3_bucket_lifecycle_configuration" "state" {
  bucket = aws_s3_bucket.state.id

  rule {
    id     = "expirar-versoes-antigas"
    status = "Enabled"

    filter {}

    noncurrent_version_expiration {
      noncurrent_days = 90
    }
  }
}

# =============================================================================
# ECR — registry da imagem da aplicação.
#
# Mora aqui, e não na stack do cluster, porque o CI precisa publicar a imagem
# mesmo quando o cluster não existe (destruído para economizar), e porque
# destruir o cluster não deve apagar as imagens já publicadas.
# =============================================================================
resource "aws_ecr_repository" "app" {
  name = var.project_name

  # Scan de vulnerabilidades a cada push. É gratuito no modo básico.
  image_scanning_configuration {
    scan_on_push = true
  }

  # A tag `latest` precisa ser sobrescrita a cada deploy; MUTABLE é obrigatório
  # para isso. As tags de versão (SHA do commit) nunca são reescritas, então na
  # prática a imutabilidade que importa é preservada pelo esquema de tags.
  image_tag_mutability = "MUTABLE"

  encryption_configuration {
    encryption_type = "AES256"
  }

  # Permite `terraform destroy` mesmo com imagens dentro. Em produção isto seria
  # `false` — aqui o repositório é reconstruível com um push.
  force_delete = true
}

# Sem isto cada deploy deixa uma imagem órfã acumulando custo de armazenamento.
# 10 versões cobrem rollback para qualquer deploy recente.
resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Mantém apenas as 10 imagens mais recentes"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = {
        type = "expire"
      }
    }]
  })
}
