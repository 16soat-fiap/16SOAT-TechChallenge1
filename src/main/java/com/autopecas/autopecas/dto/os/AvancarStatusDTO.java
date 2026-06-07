package com.autopecas.autopecas.dto.os;

import jakarta.validation.constraints.NotBlank;

public record AvancarStatusDTO(
        @NotBlank(message = "Novo status é obrigatório")
        String novoStatus,
        String observacao
) {
}
