package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.port.in.GestaoDeOrcamentos;
import com.autopecas.autopecas.application.port.in.view.OrcamentoView;
import com.autopecas.autopecas.application.port.out.MovimentacaoEstoqueRepositorio;
import com.autopecas.autopecas.application.port.out.OrcamentoRepositorio;
import com.autopecas.autopecas.application.port.out.OrdemServicoRepositorio;
import com.autopecas.autopecas.application.port.out.PecaRepositorio;
import com.autopecas.autopecas.application.port.out.Relogio;
import com.autopecas.autopecas.application.port.out.ServicoRepositorio;
import com.autopecas.autopecas.application.port.out.Transacao;
import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.estoque.MovimentacaoEstoque;
import com.autopecas.autopecas.domain.model.estoque.Peca;
import com.autopecas.autopecas.domain.model.estoque.Servico;
import com.autopecas.autopecas.domain.model.orcamento.ItemOrcamentoPeca;
import com.autopecas.autopecas.domain.model.orcamento.ItemOrcamentoServico;
import com.autopecas.autopecas.domain.model.orcamento.Orcamento;
import com.autopecas.autopecas.domain.model.os.ItemPecaOS;
import com.autopecas.autopecas.domain.model.os.ItemServicoOS;
import com.autopecas.autopecas.domain.model.os.OrdemServico;
import com.autopecas.autopecas.domain.service.MovimentadorDeEstoque;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Casos de uso do agregado Orcamento.
 *
 * <p>A aprovação é a operação de maior alcance: aprova a versão, avança a OS para EM_EXECUCAO,
 * copia os itens do orçamento para a OS e baixa o estoque de cada peça. Tudo dentro de uma
 * única transação, com gravação explícita de cada agregado alterado — incluindo as peças, que
 * antes dependiam do dirty checking do Hibernate.
 */
public class GestaoDeOrcamentosUseCase implements GestaoDeOrcamentos {

    private static final int QUANTIDADE_PADRAO = 1;

    private final OrcamentoRepositorio orcamentoRepositorio;
    private final OrdemServicoRepositorio ordemServicoRepositorio;
    private final ServicoRepositorio servicoRepositorio;
    private final PecaRepositorio pecaRepositorio;
    private final MovimentacaoEstoqueRepositorio movimentacaoRepositorio;
    private final MovimentadorDeEstoque movimentadorDeEstoque;
    private final Relogio relogio;
    private final Transacao transacao;

    public GestaoDeOrcamentosUseCase(OrcamentoRepositorio orcamentoRepositorio,
                                     OrdemServicoRepositorio ordemServicoRepositorio,
                                     ServicoRepositorio servicoRepositorio,
                                     PecaRepositorio pecaRepositorio,
                                     MovimentacaoEstoqueRepositorio movimentacaoRepositorio,
                                     MovimentadorDeEstoque movimentadorDeEstoque,
                                     Relogio relogio,
                                     Transacao transacao) {
        this.orcamentoRepositorio = orcamentoRepositorio;
        this.ordemServicoRepositorio = ordemServicoRepositorio;
        this.servicoRepositorio = servicoRepositorio;
        this.pecaRepositorio = pecaRepositorio;
        this.movimentacaoRepositorio = movimentacaoRepositorio;
        this.movimentadorDeEstoque = movimentadorDeEstoque;
        this.relogio = relogio;
        this.transacao = transacao;
    }

    @Override
    public List<OrcamentoView> daOrdemServico(UUID ordemServicoId) {
        if (!ordemServicoRepositorio.existePorId(ordemServicoId)) {
            throw new ResourceNotFoundException(
                    "Ordem de serviço não encontrada com ID: " + ordemServicoId);
        }
        return orcamentoRepositorio.daOrdemServico(ordemServicoId).stream().map(this::paraView).toList();
    }

    @Override
    public OrcamentoView criar(UUID ordemServicoId, Criar comando) {
        return transacao.executar(() -> {
            if (!ordemServicoRepositorio.existePorId(ordemServicoId)) {
                throw new ResourceNotFoundException("OS não encontrada");
            }
            if (orcamentoRepositorio.existeAprovadoParaOrdemServico(ordemServicoId)) {
                throw new BusinessException("Já existe um orçamento aprovado para esta OS");
            }

            Orcamento orcamento = Orcamento.criar(ordemServicoId,
                    orcamentoRepositorio.proximaVersao(ordemServicoId), comando.condicoesPagamento(),
                    comando.prazoExecucaoDias(), comando.dataValidade(), comando.observacoes(), null);

            adicionarItensDeServico(orcamento, comando.itensServico());
            adicionarItensDePeca(orcamento, comando.itensPeca());

            orcamento.recalcular();
            return paraView(orcamentoRepositorio.salvar(orcamento));
        });
    }

    @Override
    public OrcamentoView enviar(UUID ordemServicoId, UUID orcamentoId) {
        return transacao.executar(() -> {
            Orcamento orcamento = buscarOrcamentoDaOS(ordemServicoId, orcamentoId);
            orcamento.enviar(relogio.agora());
            return paraView(orcamentoRepositorio.salvar(orcamento));
        });
    }

    @Override
    public OrcamentoView aprovar(UUID ordemServicoId, UUID orcamentoId) {
        return transacao.executar(() -> {
            Orcamento orcamento = buscarOrcamentoDaOS(ordemServicoId, orcamentoId);

            if (orcamentoRepositorio.existeAprovadoParaOrdemServico(ordemServicoId)) {
                throw new BusinessException("Já existe um orçamento aprovado para esta OS.");
            }

            LocalDateTime agora = relogio.agora();
            orcamento.aprovar(agora);

            OrdemServico os = ordemServicoRepositorio.porId(orcamento.getOrdemServicoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ordem de serviço não encontrada com ID: " + orcamento.getOrdemServicoId()));

            os.registrarAprovacaoDeOrcamento(orcamento.getValorTotal(), agora);

            copiarItensParaOrdemServico(orcamento, os);

            orcamentoRepositorio.salvar(orcamento);
            ordemServicoRepositorio.salvar(os);

            return paraView(orcamento);
        });
    }

    @Override
    public OrcamentoView rejeitar(UUID ordemServicoId, UUID orcamentoId, String motivo) {
        return transacao.executar(() -> {
            Orcamento orcamento = buscarOrcamentoDaOS(ordemServicoId, orcamentoId);
            orcamento.rejeitar(motivo, relogio.agora());
            return paraView(orcamentoRepositorio.salvar(orcamento));
        });
    }

    private void adicionarItensDeServico(Orcamento orcamento, List<Criar.ItemServico> itens) {
        if (itens == null) {
            return;
        }
        for (Criar.ItemServico item : itens) {
            Servico servico = servicoRepositorio.porId(item.servicoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Serviço não encontrado com ID: " + item.servicoId()));
            orcamento.adicionarItemServico(ItemOrcamentoServico.criar(servico.getId(),
                    quantidadeOuPadrao(item.quantidade()), servico.getPrecoBase()));
        }
    }

    private void adicionarItensDePeca(Orcamento orcamento, List<Criar.ItemPeca> itens) {
        if (itens == null) {
            return;
        }
        for (Criar.ItemPeca item : itens) {
            Peca peca = pecaRepositorio.porId(item.pecaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Peça não encontrada com ID: " + item.pecaId()));
            orcamento.adicionarItemPeca(ItemOrcamentoPeca.criar(peca.getId(),
                    quantidadeOuPadrao(item.quantidade()), peca.getPrecoVenda()));
        }
    }

    /**
     * Copia os itens aprovados para a OS. As peças, além de virarem itens da OS, baixam o
     * estoque — e a peça alterada é gravada explicitamente.
     */
    private void copiarItensParaOrdemServico(Orcamento orcamento, OrdemServico os) {
        for (ItemOrcamentoServico item : orcamento.getItensServico()) {
            os.adicionarItemServico(ItemServicoOS.criar(item.getServicoId(), item.getQuantidade(),
                    item.getPrecoUnitario()));
        }

        for (ItemOrcamentoPeca item : orcamento.getItensPeca()) {
            os.adicionarItemPeca(ItemPecaOS.criar(item.getPecaId(), item.getQuantidade(),
                    item.getPrecoUnitario()));

            Peca peca = pecaRepositorio.porId(item.getPecaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Peça não encontrada com ID: " + item.getPecaId()));

            MovimentacaoEstoque movimentacao = movimentadorDeEstoque.movimentar(peca,
                    TipoMovimentacaoEstoque.SAIDA, item.getQuantidade(),
                    "Saída por aprovação do orçamento da OS " + os.getNumero(), os.getId(), null);

            movimentacaoRepositorio.salvar(movimentacao);
            pecaRepositorio.salvar(peca);
        }
    }

    private Orcamento buscarOrcamentoDaOS(UUID ordemServicoId, UUID orcamentoId) {
        Orcamento orcamento = orcamentoRepositorio.porId(orcamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Orcamento não encontrado"));
        if (!orcamento.getOrdemServicoId().equals(ordemServicoId)) {
            throw new BusinessException("O orçamento não pertence à OS informada.");
        }
        return orcamento;
    }

    private int quantidadeOuPadrao(Integer quantidade) {
        return quantidade != null ? quantidade : QUANTIDADE_PADRAO;
    }

    private OrcamentoView paraView(Orcamento orcamento) {
        return new OrcamentoView(orcamento.getId(), orcamento.getVersao(),
                orcamento.getStatus() != null ? orcamento.getStatus().name() : null,
                orcamento.getValorMaoObra(), orcamento.getValorPecas(), orcamento.getValorTotal(),
                orcamento.getCondicoesPagamento(), orcamento.getPrazoExecucaoDias(),
                orcamento.getDataValidade(), orcamento.getCriadoEm());
    }
}
