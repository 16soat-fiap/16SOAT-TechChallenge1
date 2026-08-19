package com.autopecas.autopecas.application.port.in;

import com.autopecas.autopecas.application.port.in.view.OrcamentoView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Inbound port do agregado Orcamento.
 *
 * <p>Aprovar um orçamento é a operação de maior alcance do sistema: além de aprovar a versão,
 * avança a OS para EM_EXECUCAO, copia os itens para a OS e baixa o estoque das peças.
 */
public interface GestaoDeOrcamentos {

    List<OrcamentoView> daOrdemServico(UUID ordemServicoId);

    OrcamentoView criar(UUID ordemServicoId, Criar comando);

    OrcamentoView enviar(UUID ordemServicoId, UUID orcamentoId);

    OrcamentoView aprovar(UUID ordemServicoId, UUID orcamentoId);

    OrcamentoView rejeitar(UUID ordemServicoId, UUID orcamentoId, String motivo);

    record Criar(
            List<ItemServico> itensServico,
            List<ItemPeca> itensPeca,
            String condicoesPagamento,
            Integer prazoExecucaoDias,
            LocalDate dataValidade,
            String observacoes
    ) {
        public record ItemServico(UUID servicoId, Integer quantidade) {
        }

        public record ItemPeca(UUID pecaId, Integer quantidade) {
        }
    }
}
