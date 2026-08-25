# =============================================================================
# Realm do Keycloak como ConfigMap.
#
# Fica no Terraform, e não nos manifestos, por uma razão prática: o arquivo tem
# ~70 KB e é o MESMO que o docker-compose monta em /opt/keycloak/data/import.
# Lendo-o com `file()`, Compose e Kubernetes consomem a mesma fonte e não há
# como divergirem. O kustomize não serviria aqui porque se recusa a ler
# arquivos fora do diretório da kustomization.
#
# Para aplicar os manifestos num cluster que não veio deste Terraform, crie o
# mesmo ConfigMap à mão:
#
#   kubectl create configmap keycloak-realm -n autopecas \
#     --from-file=realm-export.json=keycloak/realm-export.json
# =============================================================================
resource "kubernetes_config_map" "keycloak_realm" {
  metadata {
    name      = "keycloak-realm"
    namespace = kubernetes_namespace.autopecas.metadata[0].name
    labels = {
      "app.kubernetes.io/name"       = "keycloak"
      "app.kubernetes.io/part-of"    = "autopecas"
      "app.kubernetes.io/managed-by" = "terraform"
    }
  }

  data = {
    "realm-export.json" = file("${path.module}/../../keycloak/realm-export.json")
  }
}
