package com.autopecas.autopecas.application.port.in;

import com.autopecas.autopecas.application.port.in.view.VeiculoView;

import java.util.List;
import java.util.UUID;

/** Inbound port do agregado Veiculo. */
public interface GestaoDeVeiculos {

    List<VeiculoView> listarAtivos();

    VeiculoView porId(UUID id);

    VeiculoView porPlaca(String placa);

    List<VeiculoView> doCliente(UUID clienteId);

    VeiculoView cadastrar(Cadastrar comando);

    VeiculoView atualizar(UUID id, AtualizarDados comando);

    void desativar(UUID id);

    record Cadastrar(
            UUID clienteId,
            String placa,
            String chassi,
            String renavam,
            String marca,
            String modelo,
            Integer anoModelo,
            String cor
    ) {
    }

    /** Atualização parcial: campos nulos são ignorados. A placa não é alterável. */
    record AtualizarDados(
            String marca,
            String modelo,
            Integer anoModelo,
            String cor,
            String chassi,
            String renavam
    ) {
    }
}
