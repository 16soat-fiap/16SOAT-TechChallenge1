package com.autopecas.autopecas.adapter.in.web.dto.cliente;

import jakarta.validation.constraints.Email;

public record ClienteUpdateDTO(
        String nome,
        @Email
        String email,
        String telefone,
        Boolean aceitaNotificacoes
) {
}
