# =============================================================================
# Um dashboard por ambiente. Grade de 12 colunas; cada página responde a uma
# pergunta da operação:
#
#   Ordens de serviço     quanto entra por dia e quanto tempo cada etapa leva
#   API                   o cliente está sendo bem atendido (latência, erros)
#   Kubernetes            o cluster aguenta e escala (CPU, memória, HPA, nós)
#   Erros e integrações   o que está quebrando, e onde
#   Logs                  as linhas por trás de um erro, ligadas ao trace
# =============================================================================
resource "newrelic_one_dashboard" "operacao" {
  for_each = local.amb

  name        = "Autopecas - ${each.key}"
  permissions = "public_read_only"

  # ───────────────────────────────────────────────────────────────────────────
  page {
    name = "Ordens de servico"

    widget_billboard {
      title  = "OS abertas hoje"
      row    = 1
      column = 1
      width  = 4
      height = 2
      nrql_query {
        query = "FROM OrdemServicoAberta SELECT count(*) AS 'OS abertas' WHERE ${each.value.negocio} SINCE today"
      }
    }

    widget_billboard {
      title  = "Mudancas de status hoje"
      row    = 1
      column = 5
      width  = 4
      height = 2
      nrql_query {
        query = "FROM OrdemServicoStatusAlterado SELECT count(*) AS 'Transicoes' WHERE ${each.value.negocio} SINCE today"
      }
    }

    widget_billboard {
      title  = "Falhas inesperadas no processamento (24h)"
      row    = 1
      column = 9
      width  = 4
      height = 2
      nrql_query {
        query = "FROM FalhaProcessamentoOS SELECT count(*) AS 'Falhas' WHERE ${each.value.negocio} AND regraDeNegocio IS FALSE SINCE 1 day ago"
      }
      critical = 1
    }

    widget_bar {
      title  = "Volume diario de ordens de servico (30 dias)"
      row    = 3
      column = 1
      width  = 6
      height = 3
      nrql_query {
        query = "FROM OrdemServicoAberta SELECT count(*) AS 'OS abertas' WHERE ${each.value.negocio} FACET dateOf(timestamp) SINCE 30 days ago LIMIT 31"
      }
    }

    widget_bar {
      title  = "Tempo medio por status (minutos, 30 dias)"
      row    = 3
      column = 7
      width  = 6
      height = 3
      nrql_query {
        query = "FROM OrdemServicoStatusAlterado SELECT average(segundosNoStatusAnterior) / 60 AS 'Minutos' WHERE ${each.value.negocio} AND statusAnterior IN ${local.status_medidos} FACET statusAnterior SINCE 30 days ago"
      }
    }

    widget_line {
      title  = "Transicoes por status de destino"
      row    = 6
      column = 1
      width  = 6
      height = 3
      nrql_query {
        query = "FROM OrdemServicoStatusAlterado SELECT count(*) WHERE ${each.value.negocio} FACET statusNovo TIMESERIES AUTO SINCE 1 day ago"
      }
    }

    widget_table {
      title  = "Ultimas transicoes"
      row    = 6
      column = 7
      width  = 6
      height = 3
      nrql_query {
        query = "FROM OrdemServicoStatusAlterado SELECT numeroOS, statusAnterior, statusNovo, segundosNoStatusAnterior / 60 AS 'Minutos no anterior' WHERE ${each.value.negocio} SINCE 1 day ago LIMIT 20"
      }
    }
  }

  # ───────────────────────────────────────────────────────────────────────────
  page {
    name = "API"

    widget_billboard {
      title  = "Latencia p95 (s)"
      row    = 1
      column = 1
      width  = 3
      height = 2
      nrql_query {
        query = "FROM Transaction SELECT percentile(duration, 95) AS 'p95' WHERE ${each.value.apm} SINCE 30 minutes ago"
      }
      warning  = var.latencia_p95_limite_segundos / 2
      critical = var.latencia_p95_limite_segundos
    }

    widget_billboard {
      title  = "Requisicoes por minuto"
      row    = 1
      column = 4
      width  = 3
      height = 2
      nrql_query {
        query = "FROM Transaction SELECT rate(count(*), 1 minute) AS 'rpm' WHERE ${each.value.apm} SINCE 30 minutes ago"
      }
    }

    widget_billboard {
      title  = "Respostas 5xx (%)"
      row    = 1
      column = 7
      width  = 3
      height = 2
      nrql_query {
        query = "FROM Transaction SELECT percentage(count(*), WHERE http.statusCode >= 500) AS '5xx' WHERE ${each.value.apm} SINCE 30 minutes ago"
      }
      warning  = 1
      critical = var.taxa_erro_limite_percentual
    }

    widget_billboard {
      title  = "Disponibilidade do healthcheck (24h, %)"
      row    = 1
      column = 10
      width  = 3
      height = 2
      nrql_query {
        query = "FROM SyntheticCheck SELECT percentage(count(*), WHERE result = 'SUCCESS') AS 'Uptime' WHERE ${each.value.monitor} SINCE 1 day ago"
      }
    }

    widget_line {
      title  = "Latencia (s): p50, p95, p99"
      row    = 3
      column = 1
      width  = 6
      height = 3
      nrql_query {
        query = "FROM Transaction SELECT percentile(duration, 50, 95, 99) WHERE ${each.value.apm} TIMESERIES AUTO SINCE 3 hours ago"
      }
    }

    widget_line {
      title  = "Requisicoes por minuto, por status HTTP"
      row    = 3
      column = 7
      width  = 6
      height = 3
      nrql_query {
        query = "FROM Transaction SELECT rate(count(*), 1 minute) WHERE ${each.value.apm} FACET http.statusCode TIMESERIES AUTO SINCE 3 hours ago"
      }
    }

    widget_table {
      title  = "Endpoints mais lentos"
      row    = 6
      column = 1
      width  = 6
      height = 3
      nrql_query {
        query = "FROM Transaction SELECT average(duration) AS 'Media (s)', percentile(duration, 95) AS 'p95 (s)', count(*) AS 'Chamadas' WHERE ${each.value.apm} FACET name SINCE 3 hours ago LIMIT 10"
      }
    }

    widget_line {
      title  = "Tempo de resposta do healthcheck (ms)"
      row    = 6
      column = 7
      width  = 6
      height = 3
      nrql_query {
        query = "FROM SyntheticCheck SELECT average(duration) WHERE ${each.value.monitor} FACET locationLabel TIMESERIES AUTO SINCE 1 day ago"
      }
    }
  }

  # ───────────────────────────────────────────────────────────────────────────
  page {
    name = "Kubernetes"

    widget_line {
      title  = "CPU por pod da API (cores)"
      row    = 1
      column = 1
      width  = 6
      height = 3
      nrql_query {
        query = "FROM K8sContainerSample SELECT average(cpuUsedCores) WHERE ${each.value.api} FACET podName TIMESERIES AUTO SINCE 1 hour ago"
      }
    }

    widget_line {
      title  = "Memoria por pod da API (MiB)"
      row    = 1
      column = 7
      width  = 6
      height = 3
      nrql_query {
        query = "FROM K8sContainerSample SELECT average(memoryWorkingSetBytes) / 1048576 WHERE ${each.value.api} FACET podName TIMESERIES AUTO SINCE 1 hour ago"
      }
    }

    widget_line {
      title  = "Replicas do HPA: atuais, desejadas e maximo"
      row    = 4
      column = 1
      width  = 6
      height = 3
      nrql_query {
        query = "FROM K8sHpaSample SELECT latest(currentReplicas) AS 'Atuais', latest(desiredReplicas) AS 'Desejadas', latest(maxReplicas) AS 'Maximo' WHERE ${each.value.k8s} TIMESERIES AUTO SINCE 1 hour ago"
      }
    }

    widget_line {
      title  = "Nos do cluster (Cluster Autoscaler)"
      row    = 4
      column = 7
      width  = 6
      height = 3
      nrql_query {
        query = "FROM K8sNodeSample SELECT uniqueCount(nodeName) AS 'Nos' WHERE clusterName = '${var.cluster_name}' TIMESERIES AUTO SINCE 1 hour ago"
      }
    }

    widget_table {
      title  = "Reinicios de container da API"
      row    = 7
      column = 1
      width  = 6
      height = 3
      nrql_query {
        query = "FROM K8sContainerSample SELECT max(restartCount) AS 'Reinicios', latest(status) AS 'Estado' WHERE ${each.value.api} FACET podName SINCE 1 day ago"
      }
    }

    widget_table {
      title  = "Eventos do namespace"
      row    = 7
      column = 7
      width  = 6
      height = 3
      nrql_query {
        query = "FROM InfrastructureEvent SELECT event.reason, event.message, event.involvedObject.name WHERE category = 'kubernetes' AND clusterName = '${var.cluster_name}' AND event.involvedObject.namespace = '${each.value.namespace}' SINCE 1 day ago LIMIT 30"
      }
    }
  }

  # ───────────────────────────────────────────────────────────────────────────
  page {
    name = "Erros e integracoes"

    widget_line {
      title  = "Falhas de integracao (e-mail e outras)"
      row    = 1
      column = 1
      width  = 6
      height = 3
      nrql_query {
        query = "FROM FalhaIntegracao SELECT count(*) WHERE ${each.value.negocio} FACET integracao, operacao TIMESERIES AUTO SINCE 1 day ago"
      }
    }

    widget_bar {
      title  = "Falhas no processamento de OS, por operacao"
      row    = 1
      column = 7
      width  = 6
      height = 3
      nrql_query {
        query = "FROM FalhaProcessamentoOS SELECT count(*) WHERE ${each.value.negocio} FACET operacao, if(regraDeNegocio, 'regra de negocio', 'erro inesperado') SINCE 1 day ago"
      }
    }

    widget_line {
      title  = "Chamadas externas com erro (banco, JWKS, SMTP)"
      row    = 4
      column = 1
      width  = 6
      height = 3
      nrql_query {
        query = "FROM Span SELECT count(*) WHERE ${each.value.apm} AND span.kind = 'client' AND error IS TRUE FACET name TIMESERIES AUTO SINCE 1 day ago"
      }
    }

    widget_table {
      title  = "Erros da aplicacao por tipo"
      row    = 4
      column = 7
      width  = 6
      height = 3
      nrql_query {
        query = "FROM TransactionError SELECT count(*) AS 'Ocorrencias', latest(error.message) AS 'Ultima mensagem' WHERE ${each.value.apm} FACET error.class, transactionName SINCE 1 day ago LIMIT 20"
      }
    }
  }

  # ───────────────────────────────────────────────────────────────────────────
  page {
    name = "Logs"

    widget_table {
      title  = "Erros e avisos (com trace e correlationId)"
      row    = 1
      column = 1
      width  = 12
      height = 4
      nrql_query {
        query = "FROM Log SELECT level, message, context.correlationId, trace.id WHERE ${each.value.logs} AND level IN ('ERROR', 'WARN') SINCE 1 hour ago LIMIT 50"
      }
    }

    widget_line {
      title  = "Volume de logs por nivel"
      row    = 5
      column = 1
      width  = 12
      height = 3
      nrql_query {
        query = "FROM Log SELECT count(*) WHERE ${each.value.logs} FACET level TIMESERIES AUTO SINCE 3 hours ago"
      }
    }
  }
}
