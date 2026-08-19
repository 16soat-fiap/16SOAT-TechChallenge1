package com.autopecas.autopecas.adapter.out.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma linha por OS concluída com mecânico responsável, para o cálculo de tempo médio do
 * dashboard. O cálculo em si fica no caso de uso, usando a regra do domínio.
 */
public record ExecucaoConcluida(
        UUID mecanicoId,
        String mecanicoNome,
        LocalDateTime dataInicioExecucao,
        LocalDateTime dataFinalizacao
) {
}
