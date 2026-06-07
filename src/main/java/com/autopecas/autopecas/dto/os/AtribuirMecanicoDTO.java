package com.autopecas.autopecas.dto.os;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record AtribuirMecanicoDTO(
        @NotBlank(message = "Mecânico é obrigatório")
        UUID mecanicoId
) {
}
