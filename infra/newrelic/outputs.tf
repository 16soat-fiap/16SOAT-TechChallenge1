output "dashboards" {
  description = "Link de cada dashboard, por ambiente."
  value       = { for nome, d in newrelic_one_dashboard.operacao : nome => d.permalink }
}

output "monitores_health" {
  description = "URL monitorada por ambiente."
  value       = { for nome, m in newrelic_synthetics_monitor.health : nome => m.uri }
}
