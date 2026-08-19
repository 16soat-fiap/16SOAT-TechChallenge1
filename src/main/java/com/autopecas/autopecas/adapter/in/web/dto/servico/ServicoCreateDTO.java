package com.autopecas.autopecas.adapter.in.web.dto.servico;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ServicoCreateDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,
        String descricao,

        @NotNull(message = "Preço base é obrigatório")
        @Positive(message = "Preço base deve ser positivo")
        BigDecimal precoBase,

        Integer tempoEstimadoMinutos

) {
}
