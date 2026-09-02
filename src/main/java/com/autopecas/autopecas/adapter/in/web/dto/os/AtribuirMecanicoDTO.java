package com.autopecas.autopecas.adapter.in.web.dto.os;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record AtribuirMecanicoDTO(
        @NotNull(message = "Mecânico é obrigatório")
        UUID mecanicoId
) {
}
