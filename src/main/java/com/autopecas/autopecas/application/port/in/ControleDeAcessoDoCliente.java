package com.autopecas.autopecas.application.port.in;

import java.util.UUID;

/** Verifica propriedade a partir do UUID do cliente autenticado em sub. */
public interface ControleDeAcessoDoCliente {

    boolean ehOProprioCliente(String subject, UUID clienteId);

    boolean ehDonoDoVeiculo(String subject, UUID veiculoId);

    boolean ehDonoDaOrdemServico(String subject, UUID ordemServicoId);

    boolean ehDonoDaOrdemServicoPorNumero(String subject, String numero);
}
