package com.autopecas.autopecas.application.port.out;

import com.autopecas.autopecas.domain.model.estoque.Servico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de saída do catálogo de serviços. */
public interface ServicoRepositorio {

    Servico salvar(Servico servico);

    Optional<Servico> porId(UUID id);

    List<Servico> ativos();
}
