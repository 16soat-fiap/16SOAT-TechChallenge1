package com.autopecas.autopecas.adapter.out.persistence.mapper;

import com.autopecas.autopecas.adapter.out.persistence.entity.ItemPecaOSJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.entity.ItemServicoOSJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.entity.OrdemServicoJpaEntity;
import com.autopecas.autopecas.domain.model.os.ItemPecaOS;
import com.autopecas.autopecas.domain.model.os.ItemServicoOS;
import com.autopecas.autopecas.domain.model.os.OrdemServico;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Conversão entre o agregado OrdemServico e sua entidade JPA, incluindo os itens filhos.
 *
 * <p>Ao aplicar o estado sobre uma entidade carregada, os itens são substituídos preservando a
 * instância da coleção — exigência do Hibernate quando há orphanRemoval. Os ids dos itens
 * existentes são mantidos pela re-hidratação, para que a gravação atualize as linhas em vez de
 * inserir duplicatas.
 */
@Component
public class OrdemServicoJpaMapper {

    public OrdemServico paraDominio(OrdemServicoJpaEntity entidade) {
        List<ItemServicoOS> itensServico = entidade.getItensServico().stream()
                .map(this::itemServicoParaDominio)
                .toList();
        List<ItemPecaOS> itensPeca = entidade.getItensPeca().stream()
                .map(this::itemPecaParaDominio)
                .toList();

        return OrdemServico.reconstituir(entidade.getId(), entidade.getNumero(), entidade.getVersion(),
                entidade.getStatus(), entidade.getQuilometragemEntrada(),
                entidade.getObservacoesEntrada(), entidade.getDiagnostico(), entidade.getQueixaCliente(),
                entidade.getValorTotalAprovado(), entidade.getDataInicioExecucao(),
                entidade.getDataFinalizacao(), entidade.getDataEntrega(), entidade.getCreatedAt(),
                entidade.getUpdatedAt(), entidade.getClienteId(), entidade.getVeiculoId(),
                entidade.getAtendenteRecepcaoId(), entidade.getAtendenteEntregaId(),
                entidade.getMecanicoResponsavelId(), itensServico, itensPeca);
    }

    /** Cria a entidade de uma OS ainda não persistida. */
    public OrdemServicoJpaEntity novaEntidade(OrdemServico os) {
        OrdemServicoJpaEntity entidade = new OrdemServicoJpaEntity();
        entidade.setNumero(os.getNumero());
        entidade.setClienteId(os.getClienteId());
        entidade.setVeiculoId(os.getVeiculoId());
        aplicar(os, entidade);
        return entidade;
    }

    /** Aplica o estado do agregado sobre uma entidade já carregada. Número e OS-cliente são imutáveis. */
    public void aplicar(OrdemServico os, OrdemServicoJpaEntity entidade) {
        entidade.setStatus(os.getStatus());
        entidade.setQuilometragemEntrada(os.getQuilometragemEntrada());
        entidade.setObservacoesEntrada(os.getObservacoesEntrada());
        entidade.setDiagnostico(os.getDiagnostico());
        entidade.setQueixaCliente(os.getQueixaCliente());
        entidade.setValorTotalAprovado(os.getValorTotalAprovado());
        entidade.setDataInicioExecucao(os.getDataInicioExecucao());
        entidade.setDataFinalizacao(os.getDataFinalizacao());
        entidade.setDataEntrega(os.getDataEntrega());
        entidade.setAtendenteRecepcaoId(os.getAtendenteRecepcaoId());
        entidade.setAtendenteEntregaId(os.getAtendenteEntregaId());
        entidade.setMecanicoResponsavelId(os.getMecanicoResponsavelId());

        entidade.substituirItensServico(os.getItensServico().stream()
                .map(this::itemServicoParaEntidade)
                .toList());
        entidade.substituirItensPeca(os.getItensPeca().stream()
                .map(this::itemPecaParaEntidade)
                .toList());
    }

    private ItemServicoOS itemServicoParaDominio(ItemServicoOSJpaEntity entidade) {
        return ItemServicoOS.reconstituir(entidade.getId(), entidade.getServicoId(),
                entidade.getQuantidade(), entidade.getPrecoUnitario(), entidade.getStatus(),
                entidade.getExecutadoPorId(), entidade.getDataInicioExecucao(),
                entidade.getDataFimExecucao(), entidade.getObservacao());
    }

    private ItemServicoOSJpaEntity itemServicoParaEntidade(ItemServicoOS item) {
        ItemServicoOSJpaEntity entidade = new ItemServicoOSJpaEntity();
        entidade.setId(item.getId());
        entidade.setServicoId(item.getServicoId());
        entidade.setQuantidade(item.getQuantidade());
        entidade.setPrecoUnitario(item.getPrecoUnitario());
        entidade.setStatus(item.getStatus());
        entidade.setExecutadoPorId(item.getExecutadoPorId());
        entidade.setDataInicioExecucao(item.getDataInicioExecucao());
        entidade.setDataFimExecucao(item.getDataFimExecucao());
        entidade.setObservacao(item.getObservacao());
        return entidade;
    }

    private ItemPecaOS itemPecaParaDominio(ItemPecaOSJpaEntity entidade) {
        return ItemPecaOS.reconstituir(entidade.getId(), entidade.getPecaId(), entidade.getQuantidade(),
                entidade.getPrecoUnitario(), entidade.getStatus(), entidade.getInstaladoPorId(),
                entidade.getDataInstalacao());
    }

    private ItemPecaOSJpaEntity itemPecaParaEntidade(ItemPecaOS item) {
        ItemPecaOSJpaEntity entidade = new ItemPecaOSJpaEntity();
        entidade.setId(item.getId());
        entidade.setPecaId(item.getPecaId());
        entidade.setQuantidade(item.getQuantidade());
        entidade.setPrecoUnitario(item.getPrecoUnitario());
        entidade.setStatus(item.getStatus());
        entidade.setInstaladoPorId(item.getInstaladoPorId());
        entidade.setDataInstalacao(item.getDataInstalacao());
        return entidade;
    }
}
