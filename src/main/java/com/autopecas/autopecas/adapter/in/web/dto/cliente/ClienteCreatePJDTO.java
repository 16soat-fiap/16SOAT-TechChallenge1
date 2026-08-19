package com.autopecas.autopecas.adapter.in.web.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClienteCreatePJDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,
        @Email
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
