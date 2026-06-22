package com.autopecas.autopecas.util.test;

import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.domain.entity.Veiculo;

import java.util.UUID;

public class VeiculoBuilder {

    public static Veiculo.VeiculoBuilder veiculo(Cliente cliente) {
        return Veiculo.builder()
                .id(UUID.randomUUID())
                .placa("ABC1B23")
                .chassi("1HGDM28153A000001")
                .renavam("12345678901")
                .marca("Chevrolet")
                .modelo("Onix")
                .anoModelo(2020)
                .cor("Preto")
                .observacoes("Veículo em bom estado")
                .ativo(true)
                .cliente(cliente);
    }
}
