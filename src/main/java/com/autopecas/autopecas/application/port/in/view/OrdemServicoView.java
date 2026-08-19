package com.autopecas.autopecas.application.port.in.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projeção de leitura de uma Ordem de Serviço, já com o nome do cliente e a placa do veículo
 * resolvidos.
 *
 * <p>Nas listagens é montada diretamente por projeção SQL na query port, evitando N+1.
 */
public record OrdemServicoView(
        UUID id,
        String numero,
        String status,
        String queixaCliente,
        String diagnostico,
        BigDecimal valorTotalAprovado,
        LocalDateTime criadoEm,
        UUID clienteId,
        String clienteNome,
        UUID veiculoId,
        String veiculoPlaca
) {
}
