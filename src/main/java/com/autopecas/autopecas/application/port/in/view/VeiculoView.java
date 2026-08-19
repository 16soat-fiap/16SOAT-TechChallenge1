package com.autopecas.autopecas.application.port.in.view;

import java.util.UUID;

/** Projeção de leitura de veículo exposta pelos casos de uso. */
public record VeiculoView(
        UUID id,
        String placa,
        String marca,
        String modelo,
        Integer anoModelo,
        String cor,
        boolean ativo,
        UUID clienteId
) {
}
