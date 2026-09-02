package com.autopecas.autopecas.adapter.out.persistence.projection;

import com.autopecas.autopecas.domain.enums.StatusOS;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projeção de leitura da OS, montada por constructor expression em JPQL com join direto em
 * clientes e veiculos. Traz o nome do cliente e a placa em uma única consulta, sem N+1.
 */
public record OrdemServicoResumo(
        UUID id,
        String numero,
        StatusOS status,
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
