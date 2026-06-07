package com.autopecas.autopecas.dto.dashboard;

import java.math.BigDecimal;

public record DashboardDTO(
        long totalOsAbertas,
        long totalOsEmExecucao,
        long totalOsFinalizadas,
        long totalOsEntregues,
        BigDecimal receitaMes,
        long estoqueBaixoCount
) {
}
