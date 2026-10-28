# =============================================================================
# PostgreSQL — banco da aplicação.
#
# StatefulSet e não Deployment: o banco tem identidade e disco. O StatefulSet
# garante nome estável (postgres-0) e liga o pod sempre ao MESMO PVC, enquanto
# um Deployment poderia recriar o pod apontando para outro volume.
#
# Réplica única e sem replicação — é ambiente de desenvolvimento. Em produção,
# o caminho é um banco gerenciado (RDS, Cloud SQL) ou um operator (CloudNativePG,
# Zalando), não um StatefulSet artesanal.
# =============================================================================

resource "kubernetes_secret" "postgres" {
  metadata {
    name      = "postgres-credentials"
    namespace = kubernetes_namespace.autopecas.metadata[0].name
    labels = {
      "app.kubernetes.io/name"       = "postgres"
      "app.kubernetes.io/managed-by" = "terraform"
    }
  }

  data = {
    POSTGRES_USER     = var.postgres_user
    POSTGRES_PASSWORD = var.postgres_password
    POSTGRES_DB       = var.app_database
  }

  type = "Opaque"
}

resource "kubernetes_stateful_set" "postgres" {
  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace.autopecas.metadata[0].name
    labels = {
      "app.kubernetes.io/name"       = "postgres"
      "app.kubernetes.io/part-of"    = "autopecas"
      "app.kubernetes.io/managed-by" = "terraform"
    }
  }

  spec {
    service_name = "postgres"
    replicas     = 1

    selector {
      match_labels = {
        "app.kubernetes.io/name" = "postgres"
      }
    }

    template {
      metadata {
        labels = {
          "app.kubernetes.io/name"    = "postgres"
          "app.kubernetes.io/part-of" = "autopecas"
        }
      }

      spec {
        security_context {
          run_as_non_root = true
          run_as_user     = 70 # usuário postgres na imagem alpine
          fs_group        = 70
        }

        container {
          name  = "postgres"
          image = var.postgres_image

          port {
            name           = "postgres"
            container_port = 5432
          }

          env_from {
            secret_ref {
              name = kubernetes_secret.postgres.metadata[0].name
            }
          }

          # O diretório de dados precisa ser um SUBDIRETÓRIO do ponto de
          # montagem: o volume traz um `lost+found` que faz o initdb recusar o
          # diretório por não estar vazio.
          env {
            name  = "PGDATA"
            value = "/var/lib/postgresql/data/pgdata"
          }

          volume_mount {
            name       = "data"
            mount_path = "/var/lib/postgresql/data"
          }


          # pg_isready confirma que o servidor aceita conexões — mais confiável
          # que um TCP check, que responde antes do banco estar pronto.
          readiness_probe {
            exec {
              command = ["pg_isready", "-U", var.postgres_user, "-d", var.app_database]
            }
            initial_delay_seconds = 10
            period_seconds        = 5
            timeout_seconds       = 3
          }

          liveness_probe {
            exec {
              command = ["pg_isready", "-U", var.postgres_user]
            }
            initial_delay_seconds = 30
            period_seconds        = 20
            timeout_seconds       = 5
          }

          resources {
            requests = {
              cpu    = "100m"
              memory = "256Mi"
            }
            limits = {
              memory = "1Gi"
            }
          }

          security_context {
            allow_privilege_escalation = false
            capabilities {
              drop = ["ALL"]
            }
          }
        }

      }
    }

    # O PVC gerado aqui sobrevive à exclusão do StatefulSet — apagar o
    # StatefulSet não apaga os dados. Para zerar o banco é preciso remover o
    # PVC explicitamente (ver README).
    volume_claim_template {
      metadata {
        name = "data"
      }
      spec {
        access_modes = ["ReadWriteOnce"]
        resources {
          requests = {
            storage = var.postgres_storage
          }
        }
      }
    }
  }

  # Sem isto o Terraform considera o StatefulSet pronto assim que a API aceita
  # o objeto, e a aplicacao subiria antes do banco existir.
  wait_for_rollout = false
}

resource "kubernetes_service" "postgres" {
  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace.autopecas.metadata[0].name
    labels = {
      "app.kubernetes.io/name"       = "postgres"
      "app.kubernetes.io/managed-by" = "terraform"
    }
  }

  spec {
    selector = {
      "app.kubernetes.io/name" = "postgres"
    }

    port {
      name        = "postgres"
      port        = 5432
      target_port = "postgres"
    }

    # ClusterIP fixo em None (headless): o StatefulSet resolve postgres-0
    # diretamente, sem passar por balanceamento — que não faz sentido para uma
    # réplica única de banco.
    cluster_ip = "None"
  }
}
