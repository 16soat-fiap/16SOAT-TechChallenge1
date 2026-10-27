# =============================================================================
# Acesso do GitHub Actions à AWS, por OIDC — sem chave de acesso estática.
#
# OPCIONAL, e desligado por padrão (`enable_github_oidc = false`).
#
# A razão de ser opcional: quem está apenas provisionando o ambiente para
# avaliá-lo não tem — e não deveria precisar ter — um repositório configurado.
# Pior, um valor errado em `github_repository` cria um papel que ninguém
# consegue assumir, e a mensagem de erro ("not authorized to perform
# sts:AssumeRoleWithWebIdentity") não diz qual condição falhou. Melhor não criar
# o papel do que criar um papel quebrado.
#
# Quem mantém o repositório liga com:
#
#   enable_github_oidc = true
#   github_repository  = "16soat-fiap/16SOAT-TechChallenge1"
#
# ─── O que isto substitui ────────────────────────────────────────────────────
#
# Um par AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY nos secrets do repositório. A
# diferença não é de conveniência:
#
#   • a chave estática não expira — um vazamento é permanente até alguém
#     perceber e revogar;
#   • ela não sabe de onde está sendo usada: quem a copiar a usa de qualquer
#     lugar;
#   • rotacionar exige um humano lembrando de fazê-lo.
#
# Com OIDC, o Actions apresenta um token assinado pelo GitHub descrevendo
# repositório, branch e workflow, e a AWS devolve credencial temporária (1h).
# Não há segredo a vazar, e a trust policy abaixo amarra o acesso ao
# repositório informado.
#
# ─── Escopo: deploy, não provisionamento ────────────────────────────────────
#
# Este papel NÃO pode rodar `terraform apply`. Ele publica imagem no ECR e fala
# com o cluster, e nada além disso.
#
# A escolha é deliberada. Um papel capaz de aplicar as quatro stacks precisaria
# criar papéis IAM, VPCs e instâncias de banco — ou seja, seria praticamente
# administrador da conta, acessível a partir de um push. O provisionamento fica
# manual, a partir da máquina de quem tem credencial pessoal; o pipeline cuida
# do que acontece muitas vezes por dia, que é deploy de aplicação.
# =============================================================================

locals {
  oidc_habilitado = var.enable_github_oidc ? 1 : 0
}

# O thumbprint vem do certificado real do emissor, lido no momento do apply. A
# alternativa seria fixar o valor no código — e ele muda quando o GitHub
# rotaciona o certificado, quebrando a autenticação de um jeito que não tem
# relação óbvia com a causa.
data "tls_certificate" "github" {
  count = local.oidc_habilitado

  url = "https://token.actions.githubusercontent.com"
}

resource "aws_iam_openid_connect_provider" "github" {
  count = local.oidc_habilitado

  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.github[0].certificates[0].sha1_fingerprint]
}

data "aws_iam_policy_document" "github_assume" {
  count = local.oidc_habilitado

  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github[0].arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    # A condição que importa: só workflows DESTE repositório assumem o papel.
    #
    # Sem ela, qualquer repositório do GitHub — de qualquer pessoa — poderia
    # assumi-lo, porque o emissor OIDC é o mesmo para todo o GitHub. É o erro
    # mais comum nesta configuração, e o mais grave.
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repository}:${var.github_subject_suffix}"]
    }
  }
}

resource "aws_iam_role" "github_deploy" {
  count = local.oidc_habilitado

  name               = "${var.project_name}-github-deploy"
  assume_role_policy = data.aws_iam_policy_document.github_assume[0].json
  description        = "GitHub Actions: publica imagem no ECR e faz deploy no EKS"

  # 1h é o teto para credencial de OIDC federado e cobre qualquer pipeline deste
  # projeto com folga.
  max_session_duration = 3600
}

data "aws_iam_policy_document" "github_deploy" {
  count = local.oidc_habilitado

  # O token de login no ECR não é por repositório — a API não suporta restrição
  # por recurso nesta ação.
  statement {
    sid       = "LoginNoEcr"
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  # Já o push é restrito ao repositório deste projeto: o pipeline não consegue
  # sobrescrever a imagem de outro sistema na mesma conta.
  statement {
    sid    = "PushNoEcr"
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:CompleteLayerUpload",
      "ecr:DescribeImages",
      "ecr:GetDownloadUrlForLayer",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
    ]
    resources = [aws_ecr_repository.app.arn]
  }

  # Só DESCREVER o cluster — o suficiente para `aws eks update-kubeconfig`
  # montar o kubeconfig e obter token.
  #
  # Note o que este papel NÃO tem: permissão de criar, atualizar ou destruir o
  # cluster. E a permissão IAM é apenas a porta de entrada: o que o pipeline
  # pode fazer DENTRO do Kubernetes é decidido pelo access entry do EKS (ver
  # `cluster_admin_role_arns` na stack 03). Sem essa entrada, o `kubectl`
  # autentica e não autoriza nada.
  statement {
    sid    = "DescreverCluster"
    effect = "Allow"
    actions = [
      "eks:DescribeCluster",
      "eks:ListClusters",
    ]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_deploy" {
  count = local.oidc_habilitado

  name   = "deploy"
  role   = aws_iam_role.github_deploy[0].id
  policy = data.aws_iam_policy_document.github_deploy[0].json
}
