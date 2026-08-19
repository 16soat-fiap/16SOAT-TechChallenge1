package com.autopecas.autopecas.adapter.out.persistence.mapper;

import com.autopecas.autopecas.adapter.out.persistence.entity.ItemOrcamentoPecaJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.entity.ItemOrcamentoServicoJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.entity.OrcamentoJpaEntity;
import com.autopecas.autopecas.domain.model.orcamento.ItemOrcamentoPeca;
import com.autopecas.autopecas.domain.model.orcamento.ItemOrcamentoServico;
import com.autopecas.autopecas.domain.model.orcamento.Orcamento;
import org.springframework.stereotype.Component;

import java.util.List;

/** Conversão entre o agregado Orcamento e sua entidade JPA, incluindo os itens filhos. */
@Component
public class OrcamentoJpaMapper {

    public Orcamento paraDominio(OrcamentoJpaEntity entidade) {
        List<ItemOrcamentoServico> itensServico = entidade.getItensServico().stream()
                .map(this::itemServicoParaDominio)
                .toList();
        List<ItemOrcamentoPeca> itensPeca = entidade.getItensPeca().stream()
                .map(this::itemPecaParaDominio)
                .toList();

        return Orcamento.reconstituir(entidade.getId(), entidade.getVersao(), entidade.getStatus(),
                entidade.getVersionLock(), entidade.getValorMaoObra(), entidade.getValorPecas(),
                entidade.getValorAcrescimo(), entidade.getValorTotal(), entidade.getCondicoesPagamento(),
                entidade.getPrazoExecucaoDias(), entidade.getDataValidade(), entidade.getObservacoes(),
                entidade.getDataEnvio(), entidade.getDataRespostaCliente(), entidade.getMotivoRejeicao(),
                entidade.getCreatedAt(), entidade.getUpdatedAt(), entidade.getOrdemServicoId(),
                entidade.getElaboradoPorId(), itensServico, itensPeca);
    }

    /** Cria a entidade de um orçamento ainda não persistido. */
    public OrcamentoJpaEntity novaEntidade(Orcamento orcamento) {
        OrcamentoJpaEntity entidade = new OrcamentoJpaEntity();
        entidade.setVersao(orcamento.getVersao());
        entidade.setOrdemServicoId(orcamento.getOrdemServicoId());
        entidade.setElaboradoPorId(orcamento.getElaboradoPorId());
        aplicar(orcamento, entidade);
        return entidade;
    }

    /** Aplica o estado do agregado sobre uma entidade já carregada. Versão e OS são imutáveis. */
    public void aplicar(Orcamento orcamento, OrcamentoJpaEntity entidade) {
        entidade.setStatus(orcamento.getStatus());
        entidade.setValorMaoObra(orcamento.getValorMaoObra());
        entidade.setValorPecas(orcamento.getValorPecas());
        entidade.setValorAcrescimo(orcamento.getValorAcrescimo());
        entidade.setValorTotal(orcamento.getValorTotal());
        entidade.setCondicoesPagamento(orcamento.getCondicoesPagamento());
        entidade.setPrazoExecucaoDias(orcamento.getPrazoExecucaoDias());
        entidade.setDataValidade(orcamento.getDataValidade());
        entidade.setObservacoes(orcamento.getObservacoes());
        entidade.setDataEnvio(orcamento.getDataEnvio());
        entidade.setDataRespostaCliente(orcamento.getDataRespostaCliente());
        entidade.setMotivoRejeicao(orcamento.getMotivoRejeicao());

        entidade.substituirItensServico(orcamento.getItensServico().stream()
                .map(this::itemServicoParaEntidade)
                .toList());
        entidade.substituirItensPeca(orcamento.getItensPeca().stream()
                .map(this::itemPecaParaEntidade)
                .toList());
    }

    private ItemOrcamentoServico itemServicoParaDominio(ItemOrcamentoServicoJpaEntity entidade) {
        return ItemOrcamentoServico.reconstituir(entidade.getId(), entidade.getServicoId(),
                entidade.getQuantidade(), entidade.getPrecoUnitario(), entidade.getObservacao());
    }

    private ItemOrcamentoServicoJpaEntity itemServicoParaEntidade(ItemOrcamentoServico item) {
        ItemOrcamentoServicoJpaEntity entidade = new ItemOrcamentoServicoJpaEntity();
        entidade.setId(item.getId());
        entidade.setServicoId(item.getServicoId());
        entidade.setQuantidade(item.getQuantidade());
        entidade.setPrecoUnitario(item.getPrecoUnitario());
        entidade.setObservacao(item.getObservacao());
        return entidade;
    }

    private ItemOrcamentoPeca itemPecaParaDominio(ItemOrcamentoPecaJpaEntity entidade) {
        return ItemOrcamentoPeca.reconstituir(entidade.getId(), entidade.getPecaId(),
                entidade.getQuantidade(), entidade.getPrecoUnitario());
    }

    private ItemOrcamentoPecaJpaEntity itemPecaParaEntidade(ItemOrcamentoPeca item) {
        ItemOrcamentoPecaJpaEntity entidade = new ItemOrcamentoPecaJpaEntity();
        entidade.setId(item.getId());
        entidade.setPecaId(item.getPecaId());
        entidade.setQuantidade(item.getQuantidade());
        entidade.setPrecoUnitario(item.getPrecoUnitario());
        return entidade;
    }
}
