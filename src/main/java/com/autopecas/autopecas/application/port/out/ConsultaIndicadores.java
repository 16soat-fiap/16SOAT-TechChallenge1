package com.autopecas.autopecas.application.port.out;

import com.autopecas.autopecas.domain.enums.StatusOS;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Query port dos indicadores do dashboard.
 *
 * <p>Devolve linhas cruas de projeção; a média por mecânico é calculada no caso de uso,
 * reaproveitando a regra de cálculo de tempo do domínio.
 */
public interface ConsultaIndicadores {

    long contarOrdensPorStatus(StatusOS status);

    long contarPecasComEstoqueBaixo();

    /** Uma linha por OS finalizada ou entregue que tenha mecânico responsável. */
    List<ExecucaoDeMecanico> execucoesConcluidas();

    record ExecucaoDeMecanico(
            UUID mecanicoId,
            String mecanicoNome,
            LocalDateTime dataInicioExecucao,
            LocalDateTime dataFinalizacao
    ) {
    }
}
