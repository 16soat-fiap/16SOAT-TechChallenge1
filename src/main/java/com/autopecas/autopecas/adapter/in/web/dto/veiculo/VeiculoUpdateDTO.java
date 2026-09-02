package com.autopecas.autopecas.adapter.in.web.dto.veiculo;

public record VeiculoUpdateDTO(
        String marca,
        String modelo,
        Integer anoModelo,
        String cor,
        String chassi,
        String renavam
) {
}