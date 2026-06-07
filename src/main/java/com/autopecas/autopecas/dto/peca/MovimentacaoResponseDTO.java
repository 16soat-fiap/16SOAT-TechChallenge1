package com.autopecas.autopecas.dto.peca;

import java.time.LocalDateTime;
import java.util.UUID;

public record MovimentacaoResponseDTO(
        UUID id,
        String tipo,
        Integer quantidade,
        Integer saldoApos,
        String motivo,
        LocalDateTime createdAt
) {
}
