package com.autopecas.autopecas.application.port.in.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Projeção de leitura de peça, incluindo o indicador derivado de estoque baixo. */
public record PecaView(
        UUID id,
        String codigo,
        String nome,
        String descricao,
        String marca,
        BigDecimal precoVenda,
        int quantidadeEstoque,
        int quantidadeMinima,
        String unidade,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        boolean estoqueBaixo
) {
}
