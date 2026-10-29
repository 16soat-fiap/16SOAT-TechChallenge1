# =============================================================================
# Como cada ambiente aparece em cada fonte de dados. Um ambiente é:
#
#   • no APM, uma aplicação: NEW_RELIC_APP_NAME = autopecas-api-<ambiente>
#     (ConfigMap gerado pelo deploy.sh);
#   • no Kubernetes, um namespace com o mesmo nome do ambiente;
#   • nos eventos de negócio, o atributo `ambiente` (TelemetriaNewRelic).
#
# Se algum desses nomes mudar lá, muda só aqui.
# =============================================================================
locals {
  amb = {
    for nome, cfg in var.ambientes : nome => {
      app       = "autopecas-api-${nome}"
      namespace = nome

      # Filtros prontos, para as consultas não repetirem a regra.
      apm      = "appName = 'autopecas-api-${nome}'"
      negocio  = "ambiente = '${nome}'"
      k8s      = "clusterName = '${var.cluster_name}' AND namespaceName = '${nome}'"
      api      = "clusterName = '${var.cluster_name}' AND namespaceName = '${nome}' AND containerName = 'api'"
      logs     = "entity.name = 'autopecas-api-${nome}'"
      monitor  = "monitorName = 'autopecas-api-${nome} health'"
      tem_sint = cfg.health_url != null
    }
  }

  # Os status cujo tempo o dashboard compara: quanto a OS fica em diagnóstico, em
  # execução e finalizada aguardando a entrega. Medido na SAÍDA de cada status
  # (statusAnterior do evento), quando o tempo nele já está fechado.
  status_medidos = "('EM_DIAGNOSTICO', 'EM_EXECUCAO', 'FINALIZADA')"
}
