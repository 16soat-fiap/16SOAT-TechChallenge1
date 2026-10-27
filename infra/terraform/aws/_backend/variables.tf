variable "aws_region" {
  description = <<-EOT
    Região da AWS.

    us-east-1 por padrão por causa de preço: sa-east-1 (São Paulo) cobra o dobro
    pelo NAT Gateway e ~45% mais pelas instâncias EC2. Ver a tabela de custo no
    seção "Custo" do README da raiz.
  EOT
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefixo de todos os recursos. Precisa ser consistente entre as stacks."
  type        = string
  default     = "autopecas"

  validation {
    # Nome de bucket S3 e de repositório ECR só aceitam minúsculas, dígitos e
    # hífen — validar aqui dá um erro legível em vez de um erro da API da AWS.
    condition     = can(regex("^[a-z][a-z0-9-]{2,30}$", var.project_name))
    error_message = "project_name deve ter 3-31 caracteres, começar com letra e usar apenas minúsculas, dígitos e hífen."
  }
}

variable "enable_github_oidc" {
  description = <<-EOT
    Cria o provider OIDC e o papel que o GitHub Actions assume para fazer deploy.

    `false` por padrão porque quem está apenas provisionando o ambiente para
    avaliá-lo não tem um repositório configurado — e não deveria precisar ter.
    Sem este papel, o provisionamento funciona igual; o que não funciona é o
    pipeline de CD, que pode ser substituído por `infra/scripts/deploy.sh`.

    Quem mantém o repositório liga isto junto com `github_repository`.
  EOT
  type        = bool
  default     = false
}

variable "github_repository" {
  description = <<-EOT
    Repositório no formato "owner/repo". Obrigatório quando
    `enable_github_oidc = true`.

    É o que amarra o papel de deploy a um repositório específico. Errar aqui não
    dá erro de apply — dá um pipeline que não consegue assumir o papel, com uma
    mensagem de "not authorized to perform sts:AssumeRoleWithWebIdentity" que
    não diz qual condição falhou.

    Sem default de propósito: um default errado produz exatamente esse papel
    quebrado, e em silêncio.
  EOT
  type        = string
  default     = ""

  validation {
    # Validação cruzada entre variáveis (Terraform >= 1.9): o formato só é
    # exigido quando o OIDC está habilitado.
    condition     = !var.enable_github_oidc || can(regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$", var.github_repository))
    error_message = "Com enable_github_oidc = true, informe github_repository no formato \"owner/repo\" (ex.: \"16soat-fiap/16SOAT-TechChallenge1\")."
  }
}

variable "github_subject_suffix" {
  description = <<-EOT
    Que parte do repositório pode assumir o papel de deploy.

    "*" permite qualquer branch, tag, PR ou environment — conveniente enquanto
    se desenvolve o pipeline. Para apertar depois do fluxo estabilizado:

      "ref:refs/heads/main"      → só a partir do branch main
      "environment:production"   → só via um GitHub Environment, que pode exigir
                                   aprovação manual antes de rodar

    A segunda forma é a mais interessante: coloca um humano entre o push e o
    deploy, sem depender de disciplina de branch.
  EOT
  type        = string
  default     = "*"
}
