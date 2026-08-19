package com.autopecas.autopecas.application.port.out;

import com.autopecas.autopecas.domain.model.orcamento.Orcamento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de saída do agregado Orcamento. */
public interface OrcamentoRepositorio {

    Orcamento salvar(Orcamento orcamento);

    Optional<Orcamento> porId(UUID id);

    List<Orcamento> daOrdemServico(UUID ordemServicoId);

    boolean existeAprovadoParaOrdemServico(UUID ordemServicoId);

    /**
     * Próxima versão de orçamento da OS (maior versão existente mais um).
     * A unicidade final é garantida pela constraint uk_orcamento_os_versao.
     */
    int proximaVersao(UUID ordemServicoId);
}
