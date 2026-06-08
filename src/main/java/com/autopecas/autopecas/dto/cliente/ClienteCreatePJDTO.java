package com.autopecas.autopecas.dto.cliente;

import jakarta.validation.constraints.NotBlank;

public record ClienteCreatePJDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,
        String email,
        String telefone,
        boolean aceitaNotificacoes,
        @NotBlank(message = "CNPJ é obrigatório")
        String cnpj,
        @NotBlank(message = "Razão social é obrigatória")
        String razaoSocial,
        String inscricaoEstadual
) {
}
