package com.autopecas.autopecas.application.port.in.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Projeção de leitura de uma versão de orçamento. */
public record OrcamentoView(
        UUID id,
        int versao,
        String status,
        BigDecimal valorMaoObra,
        BigDecimal valorPecas,
        BigDecimal valorTotal,
        String condicoesPagamento,
        Integer prazoExecucaoDias,
        LocalDate dataValidade,
        LocalDateTime criadoEm
) {
}
