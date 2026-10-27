variable "aws_region" {
  description = "Região da AWS. Ver a seção Custo no README da raiz."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefixo de todos os recursos. Precisa ser o MESMO nas quatro stacks."
  type        = string
  default     = "autopecas"
}

# =============================================================================
# Declaradas mas NÃO usadas nesta stack.
#
# Existem para que UM único terraform.tfvars sirva às quatro stacks. Sem elas,
# o Terraform avisa "Value for undeclared variable" em todo apply desta stack —
# um aviso inofensivo que, repetido, treina quem opera a ignorar avisos.
#
# A alternativa seria um tfvars por stack, com `project_name` e `aws_region`
# repetidos em quatro arquivos. Esses dois PRECISAM ser idênticos nas quatro, e
# duplicá-los é convidar a divergência — cujo sintoma é um "recurso não
# encontrado" no meio de um apply.
# =============================================================================

variable "state_bucket" {
  description = "Usada pelas stacks 02, 03 e 04. Aceita aqui apenas para compartilhar o tfvars."
  type        = string
  default     = ""
}

variable "cluster_admin_role_arns" {
  description = "Usada pela stack 03. Aceita aqui apenas para compartilhar o tfvars."
  type        = list(string)
  default     = []
}

variable "vpc_cidr" {
  description = <<-EOT
    CIDR da VPC.

    /16 dá espaço de sobra: o VPC CNI do EKS atribui um IP da subnet a CADA POD,
    não só ao nó. Com /24 por subnet privada são ~250 IPs por AZ — suficiente
    para as réplicas deste projeto, mas é a conta que estoura primeiro quando
    um cluster EKS cresce.
  EOT
  type        = string
  default     = "10.0.0.0/16"

  validation {
    condition     = can(cidrsubnet(var.vpc_cidr, 8, 11))
    error_message = "vpc_cidr precisa ser um CIDR válido com espaço para subnets /24 (use /16 ou maior)."
  }
}

variable "app_node_port" {
  description = <<-EOT
    NodePort do Service da API — o mesmo 30080 já usado no ambiente kind
    (ver infra/k8s/30-app-deployment.yaml).

    É aqui que o NLB entrega o tráfego. Mudar este valor exige mudar o Service
    nos manifestos, senão o target group fica permanentemente unhealthy.
  EOT
  type        = number
  default     = 30080

  validation {
    condition     = var.app_node_port >= 30000 && var.app_node_port <= 32767
    error_message = "app_node_port deve estar na faixa de NodePort do Kubernetes (30000-32767)."
  }
}

variable "keycloak_node_port" {
  description = <<-EOT
    NodePort do Service do Keycloak — o mesmo 30081 do ambiente kind.

    O Keycloak continua sendo o emissor dos tokens na nuvem, e precisa ser
    alcançável de fora para que um cliente peça um token. O caminho é o mesmo da
    API: API Gateway → VPC Link → NLB → NodePort. A diferença é que a rota
    `/realms/*` NÃO passa pelo authorizer — teria que haver um token para pedir
    um token.
  EOT
  type        = number
  default     = 30081

  validation {
    condition     = var.keycloak_node_port >= 30000 && var.keycloak_node_port <= 32767
    error_message = "keycloak_node_port deve estar na faixa de NodePort do Kubernetes (30000-32767)."
  }
}
