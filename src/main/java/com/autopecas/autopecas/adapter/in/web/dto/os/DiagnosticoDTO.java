package com.autopecas.autopecas.adapter.in.web.dto.os;

import jakarta.validation.constraints.NotBlank;

public record DiagnosticoDTO(
        @NotBlank(message = "Diagnóstico é obrigatório")
        String diagnostico
) {
}
