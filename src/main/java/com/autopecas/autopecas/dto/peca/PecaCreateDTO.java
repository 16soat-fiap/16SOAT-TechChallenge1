package com.autopecas.autopecas.dto.peca;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PecaCreateDTO(
        @NotBlank(message = "Código é obrigatório")
        String codigo,

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        String descricao,

        @NotBlank(message = "Preço de custo obrigatório")
        @Positive(message = "Preço é sempre positivo")
        String categoria,

        @NotNull(message = "Preço de venda deve ser preenchido")
        @Positive(message = "Valor positivo")
        BigDecimal precoVenda,

        Integer quantidadeInicial,
        Integer quantidadeMinima,
        String unidade

) {
}
