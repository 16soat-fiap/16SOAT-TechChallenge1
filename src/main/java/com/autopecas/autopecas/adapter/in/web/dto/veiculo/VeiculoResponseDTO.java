package com.autopecas.autopecas.adapter.in.web.dto.veiculo;

import java.util.UUID;

public record VeiculoResponseDTO(
        UUID id,
        String placa,
        String marca,
        String modelo,
        Integer anoModelo,
        String cor,
        Boolean ativo,
        UUID clienteId
) {
}
