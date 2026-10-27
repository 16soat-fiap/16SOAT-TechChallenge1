# =============================================================================
# Budget mensal da conta, com alerta por e-mail.
#
# Sem filtro de custo: cobre a CONTA inteira, e não só o que tem a tag
# Project=autopecas. É o que interessa — o gasto esquecido costuma ser
# justamente o recurso que escapou da tag (um EIP solto, um snapshot do RDS).
#
# Mora nesta stack, e não no _backend, porque precisa sobreviver a qualquer
# destroy do ambiente: o momento de maior risco é depois de um `down.sh` que
# falhou no meio.
#
# Budget só de monitoramento (sem Budget Action) é gratuito.
# =============================================================================
resource "aws_budgets_budget" "mensal" {
  name         = "${var.project_name}-conta-mensal"
  budget_type  = "COST"
  limit_amount = format("%.2f", var.budget_limit_usd)
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  # Gasto REAL em três degraus. O de 50% é o que importa na prática: chega
  # cedo o bastante para derrubar o ambiente antes de estourar o limite.
  dynamic "notification" {
    for_each = [50, 80, 100]

    content {
      notification_type          = "ACTUAL"
      comparison_operator        = "GREATER_THAN"
      threshold                  = notification.value
      threshold_type             = "PERCENTAGE"
      subscriber_email_addresses = [var.budget_email]
    }
  }

  # PREVISÃO acima do limite: pega o ambiente esquecido ligado antes de o gasto
  # real chegar lá. Numa conta com pouco histórico a previsão demora a existir,
  # por isso ela complementa os alertas de gasto real e não os substitui.
  notification {
    notification_type          = "FORECASTED"
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    subscriber_email_addresses = [var.budget_email]
  }
}
