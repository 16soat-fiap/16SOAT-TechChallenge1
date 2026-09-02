package com.autopecas.autopecas.application.port.out;

import com.autopecas.autopecas.domain.model.veiculo.Veiculo;
import com.autopecas.autopecas.domain.vo.Placa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de saída do agregado Veiculo. */
public interface VeiculoRepositorio {

    Veiculo salvar(Veiculo veiculo);

    Optional<Veiculo> porId(UUID id);

    Optional<Veiculo> porIdAtivo(UUID id);

    Optional<Veiculo> porPlacaAtiva(Placa placa);

    Optional<Veiculo> porChassi(String chassi);

    Optional<Veiculo> porRenavam(String renavam);

    List<Veiculo> ativos();

    List<Veiculo> ativosDoCliente(UUID clienteId);
}
