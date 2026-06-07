package com.autopecas.autopecas.dto.os;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrdemServicoResponseDTO(
        UUID id,
        String numero,
        String status,
        String queixaCliente,
        String diagnostico,
        BigDecimal valorTotalAprovado,
        LocalDateTime createAt,
        UUID clienteId,
        String clienteNome,
        UUID veiculoId,
        String veiculoPlaca
) {
}
