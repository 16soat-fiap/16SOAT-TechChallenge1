package com.autopecas.autopecas.dto.cliente;

public record ClienteUpdateDTO(
        String nome,
        String email,
        String telefone,
        Boolean aceitaNotificacoes
) {
}
