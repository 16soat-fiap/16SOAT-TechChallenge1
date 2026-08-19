package com.autopecas.autopecas.adapter.in.web.dto.dashboard;

public record DashboardDTO(
        long totalOsAbertas,
        long totalOsEmExecucao,
        long totalOsFinalizadas,
        long totalOsEntregues,
        long estoqueBaixoCount
) {
}
