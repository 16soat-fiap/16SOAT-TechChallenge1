package com.autopecas.autopecas.application.port.in.view;

import java.util.UUID;

/** Projeção de leitura de funcionário exposta pelos casos de uso. */
public record FuncionarioView(
        UUID id,
        String matricula,
        String nome,
        String email,
        String telefone,
        String tipo,
        boolean ativo
) {
}
