# =============================================================================
# NLB interno — a ponte entre o API Gateway e o cluster.
#
# `internal = true` é o ponto central do desenho de segurança: este balanceador
# não tem endereço público. Quem chega até ele é o VPC Link do API Gateway
# (stack 04), e mais ninguém.
#
# Por que NLB e não ALB: o VPC Link do API Gateway aceita os dois, mas o ALB
# traria um segundo roteador HTTP no caminho — com o seu próprio conjunto de
# regras de path, duplicando o que o API Gateway já faz. O NLB opera em camada 4
# e só encaminha, mantendo um único lugar onde roteamento é decidido.
# =============================================================================
resource "aws_lb" "internal" {
  name               = "${var.project_name}-nlb"
  internal           = true
  load_balancer_type = "network"

  subnets         = local.network.private_subnet_ids
  security_groups = [local.network.security_group_ids.nlb]

  # Sem isto, cada nó do NLB só entrega para alvos da PRÓPRIA AZ. Com 2 nós em
  # AZs diferentes, a queda de um deixaria metade das requisições sem alvo
  # saudável na sua zona. O custo é tráfego entre AZs, irrelevante neste volume.
  enable_cross_zone_load_balancing = true

  tags = {
    Name = "${var.project_name}-nlb"
  }
}

# =============================================================================
# Um target group por serviço exposto — API e Keycloak.
#
# O mapa vem da stack 01 (`node_ports`), que é a fonte única do par
# listener/NodePort. Acrescentar um serviço exposto é acrescentar uma entrada
# naquele mapa.
#
# `target_type = "instance"`: os alvos são os NÓS, na porta do NodePort, e quem
# entrega ao pod é o kube-proxy. A alternativa — registrar IPs de pod — é mais
# eficiente (um salto menos) mas exigiria o AWS Load Balancer Controller no
# cluster e um CRD TargetGroupBinding. Com `instance`, o Service NodePort que já
# existe desde a Fase 2 funciona sem nenhuma peça nova.
# =============================================================================
resource "aws_lb_target_group" "services" {
  for_each = local.network.node_ports

  name        = "${var.project_name}-${each.key}"
  vpc_id      = local.network.vpc_id
  target_type = "instance"
  port        = each.value.node_port
  protocol    = "TCP"

  # ───────────────────────────────────────────────────────────────────────────
  # Preservação do IP do cliente DESLIGADA, de propósito.
  #
  # Com ela ligada (o padrão para alvos do tipo instance), o nó vê o IP do
  # cliente original — e aí a regra de security group do nó, que libera a porta
  # para o security group do NLB, não corresponde mais à origem do pacote. O
  # resultado seria um target group permanentemente unhealthy.
  #
  # E não se perde informação: o IP real do cliente chega à aplicação no header
  # `X-Forwarded-For`, posto pelo API Gateway, que é um header HTTP e portanto
  # atravessa o NLB intacto.
  # ───────────────────────────────────────────────────────────────────────────
  preserve_client_ip = false

  # 30s para drenar conexões em voo quando um nó sai do grupo. Conversa com o
  # `terminationGracePeriodSeconds: 40` e o shutdown graceful da API.
  deregistration_delay = 30

  health_check {
    # Health check HTTP, não TCP: um TCP check passa assim que o kube-proxy
    # abre a porta no nó, muito antes de a aplicação estar pronta — e o Flyway
    # + inicialização da JVM levam dezenas de segundos. O resultado seria
    # tráfego entregue a um pod que ainda devolve erro.
    protocol = "HTTP"
    path     = each.value.health_path
    port     = "traffic-port"

    interval = 10
    # 6s é o máximo que o NLB aceita para health check HTTP, e é folgado para
    # um endpoint de readiness.
    timeout = 6

    # Os dois precisam ser IGUAIS: o NLB rejeita thresholds diferentes — ao
    # contrário do ALB, que aceita subir devagar e descer rápido.
    healthy_threshold   = 2
    unhealthy_threshold = 2

    matcher = "200-399"
  }

  tags = {
    Name = "${var.project_name}-${each.key}"
  }

  # Sem `create_before_destroy` aqui, ao contrário dos security groups: o
  # `name_prefix` de target group aceita apenas 6 caracteres, curto demais para
  # distinguir "api" de "keycloak" com o prefixo do projeto. Com nome fixo, o
  # create-before-destroy falharia por nome duplicado.
  #
  # Consequência: mudar a porta ou o protocolo exige dois applies — um
  # removendo o listener, outro recriando. Acontece praticamente nunca, e o erro
  # é explícito quando acontece.
}

# Registra os nós do cluster nos target groups conforme o Auto Scaling Group
# cresce e encolhe. É o que faz um nó criado pelo Cluster Autoscaler entrar
# automaticamente no balanceamento — sem isto, escalar a frota não aumentaria a
# capacidade de atendimento.
resource "aws_autoscaling_attachment" "services" {
  for_each = local.network.node_ports

  autoscaling_group_name = aws_eks_node_group.main.resources[0].autoscaling_groups[0].name
  lb_target_group_arn    = aws_lb_target_group.services[each.key].arn
}

resource "aws_lb_listener" "services" {
  for_each = local.network.node_ports

  load_balancer_arn = aws_lb.internal.arn
  port              = each.value.listener_port
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.services[each.key].arn
  }

  tags = {
    Name = "${var.project_name}-${each.key}"
  }
}
