package com.autopecas.autopecas.application.port.in.view;

import java.time.LocalDateTime;
import java.util.UUID;

/** Projeção de leitura de uma movimentação de estoque. */
public record MovimentacaoView(
        UUID id,
        String tipo,
        int quantidade,
        int saldoApos,
        String motivo,
        LocalDateTime criadoEm
) {
}
