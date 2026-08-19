package com.autopecas.autopecas.application.port.in;

import com.autopecas.autopecas.application.port.in.view.ServicoView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Inbound port do catálogo de serviços. */
public interface GestaoDeServicos {

    List<ServicoView> listarAtivos();

    ServicoView porId(UUID id);

    ServicoView cadastrar(DadosDoServico comando);

    ServicoView atualizar(UUID id, DadosDoServico comando);

    void desativar(UUID id);

    /** Mesmo formato para cadastro e atualização, como na API atual. */
    record DadosDoServico(
            String nome,
            String descricao,
            BigDecimal precoBase,
            Integer tempoEstimadoMinutos
    ) {
    }
}
