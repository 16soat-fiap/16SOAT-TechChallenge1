package com.autopecas.autopecas.dto.os;

import jakarta.validation.constraints.NotBlank;

public record DiagnosticoDTO(
        @NotBlank(message = "Diagnóstico é obrigatório")
        String diagnostico
) {
}
