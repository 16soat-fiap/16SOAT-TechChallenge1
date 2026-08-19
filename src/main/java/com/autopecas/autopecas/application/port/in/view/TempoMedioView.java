package com.autopecas.autopecas.application.port.in.view;

import java.util.UUID;

/** Tempo médio de execução das OS finalizadas, agrupado por mecânico responsável. */
public record TempoMedioView(
        UUID mecanicoId,
        String mecanicoNome,
        Double tempoMedioMinutos
) {
}
