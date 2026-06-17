package com.autopecas.autopecas.dto.peca;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MovimentacaoCreateDTO(
        @NotBlank(message = "Tipo é obrigatório")
        String tipo,

        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Valor deve ser positivo")
        Integer quantidade,
        String motivo
) {
}
