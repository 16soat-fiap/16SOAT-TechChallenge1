package com.autopecas.autopecas.application.port.in.view;

import java.util.UUID;

/** Projeção de leitura de cliente exposta pelos casos de uso. */
public record ClienteView(
        UUID id,
        String nome,
        String documento,
        String email,
        String telefone
) {
}
