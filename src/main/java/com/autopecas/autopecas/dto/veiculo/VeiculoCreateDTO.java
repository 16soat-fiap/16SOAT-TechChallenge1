package com.autopecas.autopecas.dto.veiculo;

import com.autopecas.autopecas.domain.entity.Cliente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VeiculoCreateDTO(
        @NotNull(message = "Cliente é obrigatório")
        UUID clienteId,

        @NotBlank(message = "Placa é obrigatória")
        String placa,
        String chassi,
        String renavam,

        @NotBlank(message = "Marca é obrigatória")
        String marca,

        @NotBlank(message = "Modelo é obrigatório")
        String modelo,

        Integer anoModelo,
        String cor
) {
}
