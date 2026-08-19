package com.autopecas.autopecas.application.port.in.view;

import java.math.BigDecimal;
import java.util.UUID;

/** Projeção de leitura de serviço do catálogo. */
public record ServicoView(
        UUID id,
        String nome,
        String descricao,
        BigDecimal precoBase,
        Integer tempoEstimadoMinutos,
        boolean ativo
) {
}
