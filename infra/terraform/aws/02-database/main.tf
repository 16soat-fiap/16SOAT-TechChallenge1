# =============================================================================
# PostgreSQL gerenciado (RDS) — substitui o StatefulSet da Fase 2.
#
# O que se ganha em relação ao StatefulSet de infra/terraform/database.tf:
# backup automático com point-in-time recovery, patch de versão sem downtime
# planejado por nós, criptografia em repouso e métricas sem instrumentar nada.
# O que se perde: rodar de graça.
#
# Nenhum banco do Keycloak aqui. Na Fase 3 a autenticação virou Lambda (stack
# 04), e com ela foi embora o `keycloak_db` — e os ~13 minutos de rollout do
# Keycloak que dominavam o pipeline.
# =============================================================================

data "terraform_remote_state" "network" {
  backend = "s3"

  config = {
    bucket = var.state_bucket
    key    = "01-network/terraform.tfstate"
    region = var.aws_region
  }
}

locals {
  network = data.terraform_remote_state.network.outputs
}

# O subnet group define em quais subnets o RDS pode colocar a instância. Só
# privadas: combinado com `publicly_accessible = false`, o banco não tem
# endereço alcançável de fora da VPC.
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db"
  subnet_ids = local.network.private_subnet_ids

  description = "Subnets privadas do ${var.project_name}"

  tags = {
    Name = "${var.project_name}-db"
  }
}

# =============================================================================
# Parameter group — TLS obrigatório.
#
# `rds.force_ssl = 1` faz o servidor RECUSAR conexão sem TLS. É a diferença
# entre "o cliente pode usar TLS" e "o cliente não tem escolha": sem isto, uma
# string de conexão mal configurada trafegaria a senha em claro dentro da VPC e
# ninguém perceberia.
#
# Exige `?sslmode=require` na URL JDBC — está montada no output `jdbc_url` e
# chega à aplicação pelo ConfigMap da stack 04. `require` cifra o tráfego sem
# validar a cadeia do certificado; validar exigiria embarcar a CA da AWS no
# truststore da JVM, o que não se justifica para tráfego que nunca sai da VPC.
# =============================================================================
resource "aws_db_parameter_group" "main" {
  # `name_prefix` pelo mesmo motivo dos security groups: a instância referencia
  # este grupo, então ele precisa nascer antes de o antigo morrer — e nome de
  # parameter group é único na conta.
  name_prefix = "${var.project_name}-pg16-"
  family      = "postgres16"

  description = "PostgreSQL 16 do ${var.project_name}"

  parameter {
    name  = "rds.force_ssl"
    value = "1"
  }

  # Registra toda consulta acima de 1s. Barato, e é o primeiro lugar onde se
  # olha quando a API fica lenta sob carga do HPA.
  parameter {
    name  = "log_min_duration_statement"
    value = "1000"
  }

  lifecycle {
    create_before_destroy = true
  }
}

# =============================================================================
# Instância.
#
# `manage_master_user_password = true` delega a senha ao RDS: ele gera, guarda
# no Secrets Manager e rotaciona. O ponto importante é que a senha NUNCA passa
# pelo Terraform — não aparece neste state, nem num tfvars, nem no histórico do
# shell. A stack 04 lê o secret para montar o Secret do Kubernetes.
# =============================================================================
resource "aws_db_instance" "main" {
  identifier = "${var.project_name}-db"

  engine = "postgres"
  # Apenas a major: a AWS escolhe a minor mais recente disponível. Fixar a minor
  # faria o Terraform querer "voltar" a versão depois de uma janela de
  # manutenção automática.
  engine_version = var.engine_version

  instance_class    = var.instance_class
  allocated_storage = var.allocated_storage

  # gp3 custa praticamente o mesmo que gp2 e entrega IOPS previsível sem
  # depender do tamanho do volume — em gp2, 20 GB rendem só 60 IOPS de baseline.
  storage_type      = "gp3"
  storage_encrypted = true

  # Teto para o autoscaling de armazenamento. Sem isto, o disco cheio derruba a
  # instância; com isto, ela cresce sozinha até o limite. O crescimento é
  # irreversível — não se reduz storage de RDS —, daí um teto conservador.
  max_allocated_storage = var.max_allocated_storage

  db_name  = var.database_name
  username = var.database_username

  manage_master_user_password = true

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [local.network.security_group_ids.rds]
  parameter_group_name   = aws_db_parameter_group.main.name
  publicly_accessible    = false

  multi_az = var.multi_az

  backup_retention_period = var.backup_retention_period
  # Janelas em UTC, escolhidas para cair de madrugada no horário de Brasília.
  backup_window      = "06:00-07:00"
  maintenance_window = "sun:07:30-sun:08:30"

  # Só a minor. Uma major (16 → 17) pode quebrar compatibilidade e exige
  # decisão humana.
  auto_minor_version_upgrade = true

  # Exporta o log do Postgres para o CloudWatch. Sem isto, o log fica apenas na
  # instância e desaparece quando ela é substituída.
  enabled_cloudwatch_logs_exports = ["postgresql"]

  # Ambos refletem "este é um ambiente de estudo que será destruído":
  # deletion_protection impediria o destroy, e um snapshot final cobraria
  # armazenamento indefinidamente. Em produção, os dois invertem.
  deletion_protection       = var.deletion_protection
  skip_final_snapshot       = var.skip_final_snapshot
  final_snapshot_identifier = var.skip_final_snapshot ? null : "${var.project_name}-db-final"

  # Mudanças valem na hora, sem esperar a janela de manutenção. Aceitável aqui
  # porque algumas provocam reinício da instância — em produção, `false`.
  apply_immediately = true

  tags = {
    Name = "${var.project_name}-db"
  }

  lifecycle {
    ignore_changes = [
      # A AWS avança a minor sozinha (auto_minor_version_upgrade). Sem isto,
      # todo plan depois de uma manutenção proporia reverter a versão.
      engine_version,
    ]
  }
}
