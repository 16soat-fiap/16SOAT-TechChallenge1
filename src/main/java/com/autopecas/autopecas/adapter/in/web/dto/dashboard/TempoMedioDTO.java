package com.autopecas.autopecas.adapter.in.web.dto.dashboard;

import java.util.UUID;

public record TempoMedioDTO(
        UUID mecanicoId,
        String mecanicoNome,
        Double tempoMedioMinutos
) {
}
