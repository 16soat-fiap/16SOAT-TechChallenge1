package com.autopecas.autopecas.application.port.in.view;

/** KPIs consolidados da oficina para o dashboard. */
public record IndicadoresView(
        long totalOsAbertas,
        long totalOsEmExecucao,
        long totalOsFinalizadas,
        long totalOsEntregues,
        long estoqueBaixoCount
) {
}
