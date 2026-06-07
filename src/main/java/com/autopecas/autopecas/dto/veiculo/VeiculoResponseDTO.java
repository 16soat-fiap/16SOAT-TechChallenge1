package com.autopecas.autopecas.dto.veiculo;

import java.util.UUID;

public record VeiculoResponseDTO(
        UUID id,
        String placa,
        String marca,
        String modelo,
        Integer anoModelo,
        String cor,
        UUID clienteId
) {
}
