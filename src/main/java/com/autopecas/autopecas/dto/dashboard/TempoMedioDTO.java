package com.autopecas.autopecas.dto.dashboard;

import java.util.UUID;

public record TempoMedioDTO(
        UUID mecanicoId,
        String mecanicoNome,
        Double tempoMedioMinutos
) {
}
