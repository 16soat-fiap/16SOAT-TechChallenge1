# =============================================================================
# Operador — o usuário IAM que roda o Terraform no lugar do root.
#
# ─── Por que não o root ──────────────────────────────────────────────────────
#
# O root não aceita restrição nenhuma: nem política, nem região, nem negação
# explícita. Uma credencial dele vazada é a conta inteira, incluindo fechá-la
# e mudar o cartão. E quem aplica a stack 03 vira o admin do cluster
# (`bootstrap_cluster_creator_admin_permissions`) — com root aplicando, o admin
# do Kubernetes é o root.
#
# ─── Como ele autentica ──────────────────────────────────────────────────────
#
# Console com MFA, e `aws login` para o terminal. NENHUMA chave de acesso: a
# credencial do terminal é temporária e nasce de um login com MFA. Senha e MFA
# ficam FORA do Terraform de propósito — a senha iria em texto claro para o
# state, e o MFA precisa do celular de quem vai usar.
#
# ─── O que "menor privilégio" quer dizer aqui ────────────────────────────────
#
# As permissões são por SERVIÇO usado pelas stacks, e não PowerUserAccess (que
# libera ~400 serviços). Dentro dos serviços de rede e computação (EC2, ELB,
# EKS, RDS...) a permissão é ampla, porque as stacks criam e destroem quase
# tudo neles; onde o nome do recurso é previsível (S3, ECR, Lambda, IAM), ela
# está restrita ao prefixo do projeto.
#
# O próximo aperto é por AÇÃO, e o jeito certo de chegar nele é empírico: depois
# do primeiro apply, o IAM Access Analyzer gera a política a partir do que o
# CloudTrail registrou. Escrever essa lista à mão, antes de qualquer apply,
# seria adivinhar.
#
# ─── O que esta política NÃO fecha ───────────────────────────────────────────
#
# Quem aplica as stacks precisa criar papéis IAM e escrever a política inline
# do autoscaler. Somado a ser admin do cluster, isso permite em tese escalar:
# reescrever a política do papel do autoscaler e usá-la de dentro de um pod.
# Fechar de vez exige permissions boundary em todos os papéis das stacks — uma
# mudança no código delas, registrada como próximo passo e não feita aqui.
# =============================================================================

data "aws_caller_identity" "atual" {}

locals {
  conta   = data.aws_caller_identity.atual.account_id
  prefixo = var.project_name

  arn_papeis = "arn:aws:iam::${local.conta}:role/${local.prefixo}-*"

  # Os papéis que as stacks criam, cada um com o ÚNICO serviço que pode
  # recebê-lo. É o que impede, por exemplo, passar o papel do autoscaler para
  # uma Lambda criada à mão.
  papel_por_servico = {
    "${local.prefixo}-eks-cluster"        = ["eks.amazonaws.com"]
    "${local.prefixo}-eks-nodes"          = ["eks.amazonaws.com", "ec2.amazonaws.com"]
    "${local.prefixo}-cluster-autoscaler" = ["pods.eks.amazonaws.com"]
    "${local.prefixo}-authorizer"         = ["lambda.amazonaws.com"]
  }

  # Políticas gerenciadas que as stacks anexam. Anexar qualquer outra —
  # AdministratorAccess, por exemplo — é negado.
  politicas_anexaveis = [
    "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy",
    "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy",
    "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy",
    "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly",
    "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
    "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole",
    "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess",
  ]

  # Papéis vinculados a serviço que a AWS cria no primeiro uso de cada serviço.
  servicos_com_slr = [
    "eks.amazonaws.com",
    "eks-nodegroup.amazonaws.com",
    "elasticloadbalancing.amazonaws.com",
    "autoscaling.amazonaws.com",
    "rds.amazonaws.com",
    "ops.apigateway.amazonaws.com",
  ]
}

resource "aws_iam_group" "operadores" {
  name = "${local.prefixo}-operadores"
}

resource "aws_iam_user" "operador" {
  name = "${local.prefixo}-operador"

  # Senha e MFA são criados no console, fora do Terraform. Sem isto, eles
  # impediriam o `destroy` de remover o usuário.
  force_destroy = true
}

resource "aws_iam_user_group_membership" "operador" {
  user   = aws_iam_user.operador.name
  groups = [aws_iam_group.operadores.name]
}

resource "aws_iam_policy" "operador" {
  name        = "${local.prefixo}-operador"
  description = "Provisiona as stacks Terraform do projeto ${local.prefixo}"
  policy      = data.aws_iam_policy_document.operador.json
}

resource "aws_iam_group_policy_attachment" "operador" {
  group      = aws_iam_group.operadores.name
  policy_arn = aws_iam_policy.operador.arn
}

# Exigida pelo `aws login` para usuário IAM (o root dispensa). Sem ela, o login
# no browser funciona e a troca pelo token do terminal é negada.
resource "aws_iam_group_policy_attachment" "aws_login" {
  group      = aws_iam_group.operadores.name
  policy_arn = "arn:aws:iam::aws:policy/SignInLocalDevelopmentAccess"
}

data "aws_iam_policy_document" "operador" {
  # ─── Serviços de rede e computação ──────────────────────────────────────
  # Amplo por serviço: as stacks criam VPC, subnets, NAT, SGs, launch
  # templates, NLB, cluster, node group, RDS e API Gateway, com nomes que a
  # própria AWS gera — não há prefixo onde restringir.
  statement {
    sid    = "ServicosDoAmbiente"
    effect = "Allow"
    actions = [
      "apigateway:*",
      "autoscaling:*",
      "ec2:*",
      "eks:*",
      "elasticloadbalancing:*",
      "logs:*",
      "rds:*",
    ]
    resources = ["*"]
  }

  # ─── Recursos com nome previsível: restritos ao prefixo ─────────────────
  statement {
    sid       = "BucketsDoProjeto"
    effect    = "Allow"
    actions   = ["s3:*"]
    resources = ["arn:aws:s3:::${local.prefixo}-*", "arn:aws:s3:::${local.prefixo}-*/*"]
  }

  statement {
    sid       = "EcrDoProjeto"
    effect    = "Allow"
    actions   = ["ecr:*"]
    resources = ["arn:aws:ecr:*:${local.conta}:repository/${local.prefixo}*"]
  }

  # Login no ECR e listagem não aceitam restrição por recurso.
  statement {
    sid       = "EcrLogin"
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken", "ecr:DescribeRepositories"]
    resources = ["*"]
  }

  statement {
    sid       = "LambdaDoProjeto"
    effect    = "Allow"
    actions   = ["lambda:*"]
    resources = ["arn:aws:lambda:*:${local.conta}:function:${local.prefixo}-*"]
  }

  # ─── Segredo do RDS ─────────────────────────────────────────────────────
  # `manage_master_user_password` faz o RDS criar o segredo no Secrets
  # Manager, com nome "rds!db-<id>" — o prefixo é do RDS, não nosso. A stack
  # 04 lê esse segredo para montar o Secret do Kubernetes.
  statement {
    sid       = "SegredoGerenciadoPeloRds"
    effect    = "Allow"
    actions   = ["secretsmanager:*"]
    resources = ["arn:aws:secretsmanager:*:${local.conta}:secret:rds!*"]
  }

  # A chave KMS padrão do Secrets Manager cifra o segredo acima. Só via os
  # serviços que a usam em nome do operador, nunca diretamente.
  statement {
    sid    = "ChaveKmsViaServico"
    effect = "Allow"
    actions = [
      "kms:CreateGrant",
      "kms:Decrypt",
      "kms:DescribeKey",
      "kms:Encrypt",
      "kms:GenerateDataKey*",
    ]
    resources = ["*"]

    condition {
      test     = "StringLike"
      variable = "kms:ViaService"
      values = [
        "secretsmanager.*.amazonaws.com",
        "rds.*.amazonaws.com",
        "ec2.*.amazonaws.com",
      ]
    }
  }

  statement {
    sid       = "KmsLeitura"
    effect    = "Allow"
    actions   = ["kms:DescribeKey", "kms:ListAliases"]
    resources = ["*"]
  }

  # ─── IAM: leitura ampla, escrita só nos papéis do projeto ───────────────
  statement {
    sid       = "IamLeitura"
    effect    = "Allow"
    actions   = ["iam:Get*", "iam:List*"]
    resources = ["*"]
  }

  statement {
    sid    = "PapeisDoProjeto"
    effect = "Allow"
    actions = [
      "iam:CreateRole",
      "iam:DeleteRole",
      "iam:DeleteRolePolicy",
      "iam:PutRolePolicy",
      "iam:TagRole",
      "iam:UntagRole",
      "iam:UpdateAssumeRolePolicy",
      "iam:UpdateRole",
      "iam:UpdateRoleDescription",
    ]
    resources = [local.arn_papeis]
  }

  statement {
    sid       = "AnexarSoPoliticasConhecidas"
    effect    = "Allow"
    actions   = ["iam:AttachRolePolicy", "iam:DetachRolePolicy"]
    resources = [local.arn_papeis]

    condition {
      test     = "ArnEquals"
      variable = "iam:PolicyARN"
      values   = local.politicas_anexaveis
    }
  }

  dynamic "statement" {
    for_each = local.papel_por_servico

    content {
      sid       = "PassRole${replace(title(replace(statement.key, "-", " ")), " ", "")}"
      effect    = "Allow"
      actions   = ["iam:PassRole"]
      resources = ["arn:aws:iam::${local.conta}:role/${statement.key}"]

      condition {
        test     = "StringEquals"
        variable = "iam:PassedToService"
        values   = statement.value
      }
    }
  }

  statement {
    sid       = "PapeisVinculadosAServico"
    effect    = "Allow"
    actions   = ["iam:CreateServiceLinkedRole"]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "iam:AWSServiceName"
      values   = local.servicos_com_slr
    }
  }

  # Provider OIDC do GitHub, da stack _backend (quando habilitado).
  statement {
    sid    = "OidcDoGithub"
    effect = "Allow"
    actions = [
      "iam:CreateOpenIDConnectProvider",
      "iam:DeleteOpenIDConnectProvider",
      "iam:TagOpenIDConnectProvider",
      "iam:UpdateOpenIDConnectProviderThumbprint",
    ]
    resources = ["arn:aws:iam::${local.conta}:oidc-provider/token.actions.githubusercontent.com"]
  }

  # ─── Leituras que os scripts fazem ──────────────────────────────────────
  # up.sh confere a cota de vCPU; o budget é visível, mas só o dono da conta
  # o altera (por esta stack).
  statement {
    sid    = "LeiturasDosScripts"
    effect = "Allow"
    actions = [
      "budgets:ViewBudget",
      "servicequotas:GetServiceQuota",
      "servicequotas:ListRequestedServiceQuotaChangeHistoryByQuota",
    ]
    resources = ["*"]
  }

  # ─── Autosserviço de credencial ─────────────────────────────────────────
  # Trocar a própria senha e cadastrar o próprio MFA. Desativar MFA fica de
  # fora: quem perder o dispositivo pede ao dono da conta.
  statement {
    sid    = "PropriaCredencial"
    effect = "Allow"
    actions = [
      "iam:ChangePassword",
      "iam:CreateVirtualMFADevice",
      "iam:EnableMFADevice",
      "iam:ResyncMFADevice",
    ]
    resources = [
      "arn:aws:iam::${local.conta}:user/$${aws:username}",
      "arn:aws:iam::${local.conta}:mfa/$${aws:username}",
    ]
  }

  statement {
    sid       = "PoliticaDeSenha"
    effect    = "Allow"
    actions   = ["iam:GetAccountPasswordPolicy"]
    resources = ["*"]
  }

  # ─── Negações explícitas ────────────────────────────────────────────────
  # Nenhum papel do projeto confia em usuário IAM, e assim deve continuar:
  # sem esta negação, reescrever a trust policy de um deles para confiar no
  # próprio operador seria o atalho de escalada mais curto.
  statement {
    sid       = "NuncaAssumirPapeisDoProjeto"
    effect    = "Deny"
    actions   = ["sts:AssumeRole"]
    resources = [local.arn_papeis]
  }

  # Instance profile próprio permitiria pôr um papel do projeto numa EC2
  # criada à mão. O node group não precisa: o EKS cria o dele.
  statement {
    sid       = "SemInstanceProfileManual"
    effect    = "Deny"
    actions   = ["iam:CreateInstanceProfile", "iam:AddRoleToInstanceProfile"]
    resources = ["*"]
  }

  # Fora das regiões permitidas, só os serviços globais funcionam.
  statement {
    sid    = "SoRegioesPermitidas"
    effect = "Deny"
    not_actions = [
      "budgets:*",
      "iam:*",
      "servicequotas:*",
      "signin:*",
      "sts:*",
    ]
    resources = ["*"]

    condition {
      test     = "StringNotEquals"
      variable = "aws:RequestedRegion"
      values   = var.allowed_regions
    }
  }
}
