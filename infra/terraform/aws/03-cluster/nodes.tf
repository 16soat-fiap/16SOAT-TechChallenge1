# =============================================================================
# Launch template dos nós.
#
# Por que existe, se o node group gerenciado funciona sem nenhum: para que os
# nós usem o NOSSO security group (`sg_nodes`, da stack 01). Sem launch
# template, o EKS anexa apenas o cluster security group que ele mesmo cria — e
# aí não há onde pendurar a regra "aceita a porta 30080 vinda do NLB".
#
# Note o que NÃO está declarado aqui: `image_id`, `user_data` e `instance_type`.
# A omissão é deliberada — é o que mantém o node group "gerenciado". O EKS
# injeta a AMI otimizada correta para a versão do Kubernetes e o script de
# bootstrap. Declarar `image_id` transferiria para nós a responsabilidade de
# acompanhar as AMIs e de escrever o bootstrap à mão.
# =============================================================================
resource "aws_launch_template" "nodes" {
  name_prefix = "${var.project_name}-nodes-"
  description = "Nos do cluster ${local.cluster_name}"

  vpc_security_group_ids = [local.network.security_group_ids.nodes]

  metadata_options {
    http_endpoint = "enabled"

    # IMDSv2 obrigatório. O IMDSv1 responde a um GET simples, o que torna uma
    # SSRF na aplicação suficiente para ler a credencial do nó; o v2 exige um
    # PUT para obter token antes, o que uma SSRF comum não consegue fazer.
    http_tokens = "required"

    # 2, e não 1: o pacote sai do pod, atravessa a bridge do container e só
    # então chega ao serviço de metadados — são dois saltos. Com 1, componentes
    # que leem a região pelo IMDS quebram. O endurecimento de verdade aqui não é
    # baixar para 1, é os pods não precisarem do IMDS — que é exatamente o que
    # o Pod Identity resolve (ver iam.tf).
    http_put_response_hop_limit = 2
  }

  # Sem `key_name`: nenhuma chave SSH nos nós. O acesso de emergência é por
  # SSM Session Manager, permitido pela AmazonSSMManagedInstanceCore no papel
  # do nó — sem porta 22 aberta e com a sessão registrada em log.

  # Monitoramento detalhado (métricas EC2 de 1 minuto) deliberadamente DESLIGADO.
  #
  # Seria útil para ver a reação do autoscaling num gráfico, mas custa ~US$ 2
  # por instância por mês e a informação que interessa aqui já vem de graça de
  # outro lugar: o metrics-server alimenta `kubectl top` e o HPA com resolução
  # de 15s, e o log do Cluster Autoscaler diz por que ele decidiu escalar.
  monitoring {
    enabled = false
  }

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name = "${local.cluster_name}-node"
    }
  }

  # O node group referencia uma VERSÃO do template. Mudar o template cria uma
  # versão nova, e o node group precisa continuar apontando para algo válido
  # durante a transição.
  lifecycle {
    create_before_destroy = true
  }
}

# =============================================================================
# Node group gerenciado.
#
# Dimensionamento — a conta, para não ser número arbitrário:
#
#   t3.medium  = 4 GiB, dos quais ~3,3 GiB alocáveis (o resto é kubelet + SO).
#   Pods de sistema por nó: aws-node, kube-proxy, pod-identity-agent, e uma
#   fatia de coredns/metrics-server/autoscaler  ≈ 600 MiB.
#   Sobram ~2,7 GiB por nó.
#
#   Cada réplica da API pede 768 MiB (medido — ver infra/k8s/40-app-hpa.yaml).
#   O Keycloak pede 512 MiB.
#
#   → 2 nós comportam as 2 réplicas iniciais + Keycloak + sistema, com espaço
#     para o HPA chegar a ~6 réplicas antes de precisar de um terceiro nó.
#
# Por que não t3.small (2 GiB, metade do preço): sobrariam ~900 MiB por nó, ou
# seja UMA réplica de 768 MiB por nó e nada mais. O HPA precisaria de um nó novo
# a cada réplica, e cada nó novo leva ~2 minutos para entrar — o autoscaling
# existiria no papel e seria inútil na demonstração.
# =============================================================================
resource "aws_eks_node_group" "main" {
  cluster_name    = aws_eks_cluster.main.name
  node_group_name = "${var.project_name}-nodes"
  node_role_arn   = aws_iam_role.nodes.arn
  subnet_ids      = local.network.private_subnet_ids

  instance_types = [var.node_instance_type]
  capacity_type  = "ON_DEMAND"

  scaling_config {
    desired_size = var.node_desired_size
    min_size     = var.node_min_size
    max_size     = var.node_max_size
  }

  update_config {
    # Um nó por vez numa atualização. Com 2 nós, substituir os dois ao mesmo
    # tempo derrubaria a aplicação inteira — o PodDisruptionBudget da API
    # (infra/k8s/30-app-deployment.yaml) até impediria a drenagem, mas o
    # resultado seria um upgrade travado em vez de um upgrade seguro.
    max_unavailable = 1
  }

  launch_template {
    id = aws_launch_template.nodes.id
    # "$Latest" e não um número: o node group acompanha a versão mais recente
    # do template sem exigir um apply extra para atualizar a referência.
    version = aws_launch_template.nodes.latest_version
  }

  # NÃO usar `app.kubernetes.io/...` aqui, apesar de ser o padrão nos
  # manifestos. Label de nó vai para o `--node-labels` do kubelet, e o kubelet
  # se RECUSA a iniciar com chave no domínio kubernetes.io fora de uma lista
  # fixa (NodeRestriction, desde o 1.16). O sintoma é traiçoeiro: o bootstrap
  # termina com "done!", a instância fica saudável, e o nó simplesmente nunca
  # aparece — o node group fica em CREATING até estourar o timeout.
  labels = {
    "projeto" = var.project_name
  }

  tags = {
    Name = "${var.project_name}-nodes"
  }

  depends_on = [
    aws_iam_role_policy_attachment.nodes,
    # Sem o CNI e o kube-proxy no cluster, o nó entra e fica NotReady para
    # sempre, sem mensagem de erro que aponte para a causa.
    aws_eks_addon.vpc_cni,
    aws_eks_addon.kube_proxy,
  ]

  lifecycle {
    ignore_changes = [
      # Depois do primeiro apply, quem manda no desired_size é o Cluster
      # Autoscaler. Sem este ignore, todo `terraform apply` devolveria a frota
      # ao valor do tfvars — desfazendo um scale-up no meio de um pico.
      scaling_config[0].desired_size,
    ]
  }
}

# =============================================================================
# Tag no Auto Scaling Group — requisito da política do autoscaler.
#
# A política IAM do Cluster Autoscaler (iam.tf) restringe as ações de ESCRITA
# aos ASGs com `kubernetes.io/cluster/<cluster> = owned`. Esta tag é o que
# satisfaz essa condição.
#
# Declarada explicitamente, e não assumida: o EKS tagueia as INSTÂNCIAS do node
# group, mas o que a condição IAM avalia é a tag do ASG. Depender de um
# comportamento implícito aqui resultaria num autoscaler que sobe saudável e
# falha com AccessDenied só na primeira vez que tenta escalar — ou seja, na
# demonstração.
# =============================================================================
resource "aws_autoscaling_group_tag" "cluster_owned" {
  autoscaling_group_name = aws_eks_node_group.main.resources[0].autoscaling_groups[0].name

  tag {
    key                 = "kubernetes.io/cluster/${local.cluster_name}"
    value               = "owned"
    propagate_at_launch = true
  }
}
