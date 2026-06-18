package com.autopecas.autopecas.dto.dashboard;

public record DashboardDTO(
        long totalOsAbertas,
        long totalOsEmExecucao,
        long totalOsFinalizadas,
        long totalOsEntregues,
        long estoqueBaixoCount
) {
}
