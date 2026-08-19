package com.autopecas.autopecas.adapter.out.persistence.mapper;

import com.autopecas.autopecas.adapter.out.persistence.entity.MovimentacaoEstoqueJpaEntity;
import com.autopecas.autopecas.domain.model.estoque.MovimentacaoEstoque;
import org.springframework.stereotype.Component;

/** Conversão da movimentação de estoque. Somente-inserção: não há método de atualização. */
@Component
public class MovimentacaoEstoqueJpaMapper {

    public MovimentacaoEstoque paraDominio(MovimentacaoEstoqueJpaEntity entidade) {
        return MovimentacaoEstoque.reconstituir(entidade.getId(), entidade.getPecaId(),
                entidade.getTipo(), entidade.getQuantidade(), entidade.getSaldoApos(),
                entidade.getValorUnitarioMomento(), entidade.getMotivo(), entidade.getOrdemServicoId(),
                entidade.getExecutadoPorId(), entidade.getCreatedAt());
    }

    public MovimentacaoEstoqueJpaEntity novaEntidade(MovimentacaoEstoque movimentacao) {
        MovimentacaoEstoqueJpaEntity entidade = new MovimentacaoEstoqueJpaEntity();
        entidade.setPecaId(movimentacao.getPecaId());
        entidade.setTipo(movimentacao.getTipo());
        entidade.setQuantidade(movimentacao.getQuantidade());
        entidade.setSaldoApos(movimentacao.getSaldoApos());
        entidade.setValorUnitarioMomento(movimentacao.getValorUnitarioMomento());
        entidade.setMotivo(movimentacao.getMotivo());
        entidade.setOrdemServicoId(movimentacao.getOrdemServicoId());
        entidade.setExecutadoPorId(movimentacao.getExecutadoPorId());
        return entidade;
    }
}
