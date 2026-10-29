# =============================================================================
# Healthcheck e uptime vistos de FORA do cluster: um monitor do tipo ping no
# /actuator/health, pelo API Gateway — o mesmo caminho de um cliente (gateway →
# VPC Link → NLB → pod). As probes do Kubernetes dizem se o pod está vivo; só
# isto diz se o serviço está alcançável.
#
# Tipo SIMPLE (ping) de propósito: na conta gratuita, monitores ping são
# ilimitados e não consomem a cota de checks dos outros tipos.
#
# O /actuator/health agrega o banco (readiness): UP aqui significa aplicação e
# RDS respondendo.
# =============================================================================
resource "newrelic_synthetics_monitor" "health" {
  for_each = { for nome, cfg in var.ambientes : nome => cfg if cfg.health_url != null }

  name   = "autopecas-api-${each.key} health"
  type   = "SIMPLE"
  status = "ENABLED"
  period = "EVERY_5_MINUTES"
  uri    = each.value.health_url

  # Duas regiões: uma falha numa só é problema da rota até o New Relic, não da
  # aplicação.
  locations_public = ["US_EAST_1", "SA_EAST_1"]

  # GET em vez de HEAD, para conferir o corpo: 200 com status DOWN não é saúde.
  validation_string   = "\"status\":\"UP\""
  bypass_head_request = true
  verify_ssl          = true
}
