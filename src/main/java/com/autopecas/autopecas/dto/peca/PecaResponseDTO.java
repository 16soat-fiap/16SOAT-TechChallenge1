package com.autopecas.autopecas.dto.peca;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PecaResponseDTO(
        UUID id,
        String codigo,
        String nome,
        String descricao,
        String marca,
        BigDecimal precoVenda,
        Integer quantidadeEstoque,
        Integer quantidadeMinima,
        String unidade,
        Boolean ativo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean estoqueBaixo
) {
}
