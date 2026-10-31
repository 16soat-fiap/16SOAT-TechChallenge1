# =============================================================================
# Alertas. Uma política para o projeto; uma condição por sinal e por ambiente.
#
#   Falha no processamento de OS   o requisito explícito: qualquer erro
#                                  INESPERADO ao abrir/avançar/diagnosticar uma
#                                  OS. Recusa por regra de negócio (transição
#                                  inválida) não alerta — é a API funcionando.
#   Latência p95                   o cliente esperando demais.
#   Respostas 5xx                  a API quebrando para quem chama.
#   Falha de integração            o e-mail ao cliente não saiu (a OS avançou,
#                                  mas ninguém foi avisado).
#   Healthcheck                    a aplicação fora do ar vista de fora, pelo
#                                  gateway — o mesmo caminho do cliente.
#
# `incident_preference = PER_CONDITION_AND_TARGET`: homologação e produção
# abrem incidentes separados, e um problema não esconde o outro.
# =============================================================================
resource "newrelic_alert_policy" "autopecas" {
  name                = "autopecas"
  incident_preference = "PER_CONDITION_AND_TARGET"
}

resource "newrelic_nrql_alert_condition" "falha_processamento_os" {
  for_each = local.amb

  policy_id   = newrelic_alert_policy.autopecas.id
  type        = "static"
  name        = "[${each.key}] Falha no processamento de ordem de servico"
  description = "Erro inesperado (nao regra de negocio) ao processar uma OS. Investigar o trace e os logs do evento FalhaProcessamentoOS."
  enabled     = true

  nrql {
    query = "FROM FalhaProcessamentoOS SELECT count(*) WHERE ${each.value.negocio} AND regraDeNegocio IS FALSE"
  }

  critical {
    operator              = "above"
    threshold             = 0
    threshold_duration    = 60
    threshold_occurrences = "at_least_once"
  }

  # Evento raro: sem dado é "nenhuma falha", não "sinal perdido". E event_timer,
  # não event_flow: no event_flow a janela só fecha quando chega um evento
  # posterior — com uma falha isolada, o incidente nunca abriria.
  fill_option                  = "static"
  fill_value                   = 0
  aggregation_window           = 60
  aggregation_method           = "event_timer"
  aggregation_timer            = 60
  violation_time_limit_seconds = 86400
}

resource "newrelic_nrql_alert_condition" "latencia_p95" {
  for_each = local.amb

  policy_id   = newrelic_alert_policy.autopecas.id
  type        = "static"
  name        = "[${each.key}] Latencia p95 da API acima de ${var.latencia_p95_limite_segundos}s"
  description = "p95 das transacoes da API acima do limite por 5 minutos seguidos."
  enabled     = true

  nrql {
    query = "FROM Transaction SELECT percentile(duration, 95) WHERE ${each.value.apm}"
  }

  critical {
    operator              = "above"
    threshold             = var.latencia_p95_limite_segundos
    threshold_duration    = 300
    threshold_occurrences = "all"
  }

  fill_option                  = "none"
  aggregation_window           = 60
  aggregation_method           = "event_flow"
  aggregation_delay            = 120
  violation_time_limit_seconds = 86400
}

resource "newrelic_nrql_alert_condition" "taxa_erro" {
  for_each = local.amb

  policy_id   = newrelic_alert_policy.autopecas.id
  type        = "static"
  name        = "[${each.key}] Respostas 5xx acima de ${var.taxa_erro_limite_percentual}%"
  description = "Percentual de respostas 5xx da API acima do limite por 5 minutos seguidos."
  enabled     = true

  nrql {
    query = "FROM Transaction SELECT percentage(count(*), WHERE http.statusCode >= 500) WHERE ${each.value.apm}"
  }

  critical {
    operator              = "above"
    threshold             = var.taxa_erro_limite_percentual
    threshold_duration    = 300
    threshold_occurrences = "all"
  }

  fill_option                  = "none"
  aggregation_window           = 60
  aggregation_method           = "event_flow"
  aggregation_delay            = 120
  violation_time_limit_seconds = 86400
}

resource "newrelic_nrql_alert_condition" "falha_integracao" {
  for_each = local.amb

  policy_id   = newrelic_alert_policy.autopecas.id
  type        = "static"
  name        = "[${each.key}] Falha de integracao"
  description = "Uma integracao externa falhou (ex.: e-mail de mudanca de status nao enviado). A operacao principal seguiu."
  enabled     = true

  nrql {
    query = "FROM FalhaIntegracao SELECT count(*) WHERE ${each.value.negocio}"
  }

  # Warning, não critical: o cliente não foi avisado, mas a oficina não parou.
  warning {
    operator              = "above"
    threshold             = 0
    threshold_duration    = 300
    threshold_occurrences = "at_least_once"
  }

  # O New Relic exige um limiar critical em toda condição; este só dispara se a
  # integração falhar em todos os minutos de 15.
  critical {
    operator              = "above"
    threshold             = 0
    threshold_duration    = 900
    threshold_occurrences = "all"
  }

  # Evento raro, como a falha de processamento: event_timer.
  fill_option                  = "static"
  fill_value                   = 0
  aggregation_window           = 60
  aggregation_method           = "event_timer"
  aggregation_timer            = 60
  violation_time_limit_seconds = 86400
}

resource "newrelic_nrql_alert_condition" "healthcheck" {
  for_each = { for nome, a in local.amb : nome => a if a.tem_sint }

  policy_id   = newrelic_alert_policy.autopecas.id
  type        = "static"
  name        = "[${each.key}] Healthcheck da API falhando"
  description = "O monitor sintetico nao recebeu UP do /actuator/health pelo gateway em duas checagens seguidas."
  enabled     = true

  nrql {
    query = "FROM SyntheticCheck SELECT filter(count(*), WHERE result != 'SUCCESS') WHERE ${each.value.monitor}"
  }

  # O monitor checa a cada 5 minutos: janela de 5 minutos, e duas janelas
  # seguidas com falha para abrir incidente — uma falha isolada é ruído de rede.
  critical {
    operator              = "above"
    threshold             = 0
    threshold_duration    = 600
    threshold_occurrences = "all"
  }

  fill_option                  = "none"
  aggregation_window           = 300
  aggregation_method           = "event_flow"
  aggregation_delay            = 120
  violation_time_limit_seconds = 86400
}

# ─────────────────────────────────────────────────────────────────────────────
# Para onde os incidentes vão: e-mail, via workflow filtrado pela política.
# ─────────────────────────────────────────────────────────────────────────────
resource "newrelic_notification_destination" "email" {
  name = "autopecas-email"
  type = "EMAIL"

  property {
    key   = "email"
    value = var.email_alertas
  }
}

resource "newrelic_notification_channel" "email" {
  name           = "autopecas-email"
  type           = "EMAIL"
  destination_id = newrelic_notification_destination.email.id
  product        = "IINT"

  property {
    key   = "subject"
    value = "[autopecas] {{ issueTitle }}"
  }
}

resource "newrelic_workflow" "alertas" {
  name                  = "autopecas-alertas"
  muting_rules_handling = "NOTIFY_ALL_ISSUES"

  issues_filter {
    name = "politica-autopecas"
    type = "FILTER"

    predicate {
      attribute = "labels.policyIds"
      operator  = "EXACTLY_MATCHES"
      values    = [newrelic_alert_policy.autopecas.id]
    }
  }

  destination {
    channel_id = newrelic_notification_channel.email.id
  }
}
