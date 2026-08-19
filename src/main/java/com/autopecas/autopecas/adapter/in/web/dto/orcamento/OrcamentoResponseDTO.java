package com.autopecas.autopecas.adapter.in.web.dto.orcamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrcamentoResponseDTO(
        UUID id,
        Integer versao,
        String status,
        BigDecimal valorMaoObra,
        BigDecimal valorPecas,
        BigDecimal valorTotal,
        String condicoesPagamento,
        Integer prazoExecucaoDias,
        LocalDate dataValidade,
        LocalDateTime createdAt
) {
}
