package com.autopecas.autopecas.adapter.in.web.dto.servico;

import java.math.BigDecimal;
import java.util.UUID;

public record ServicoResponseDTO(
        UUID id,
        String nome,
        String descricao,
        BigDecimal precoBase,
        Integer tempoEstimadoMinutos,
        Boolean ativo
) {
}
