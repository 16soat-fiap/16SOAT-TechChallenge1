package com.autopecas.autopecas.dto.os;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record AtribuirMecanicoDTO(
        @NotNull(message = "Mecânico é obrigatório")
        UUID mecanicoId
) {
}
