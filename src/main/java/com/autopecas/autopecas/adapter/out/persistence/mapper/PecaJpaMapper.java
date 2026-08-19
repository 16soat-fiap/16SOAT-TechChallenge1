package com.autopecas.autopecas.adapter.out.persistence.mapper;

import com.autopecas.autopecas.adapter.out.persistence.entity.PecaJpaEntity;
import com.autopecas.autopecas.domain.model.estoque.Peca;
import org.springframework.stereotype.Component;

/** Conversão entre o agregado Peca e sua entidade JPA. */
@Component
public class PecaJpaMapper {

    public Peca paraDominio(PecaJpaEntity entidade) {
        return Peca.reconstituir(entidade.getId(), entidade.getCodigo(), entidade.getNome(),
                entidade.getDescricao(), entidade.getMarca(), entidade.getPrecoVenda(),
                entidade.getQuantidadeEstoque(), entidade.getQuantidadeMinima(), entidade.getUnidade(),
                Boolean.TRUE.equals(entidade.getAtivo()), entidade.getCreatedAt(),
                entidade.getUpdatedAt());
    }

    /** Cria a entidade de uma peça ainda não persistida. */
    public PecaJpaEntity novaEntidade(Peca peca) {
        PecaJpaEntity entidade = new PecaJpaEntity();
        entidade.setCodigo(peca.getCodigo());
        aplicar(peca, entidade);
        return entidade;
    }

    /** Aplica o estado do agregado sobre uma entidade já carregada. O código é imutável. */
    public void aplicar(Peca peca, PecaJpaEntity entidade) {
        entidade.setNome(peca.getNome());
        entidade.setDescricao(peca.getDescricao());
        entidade.setMarca(peca.getMarca());
        entidade.setPrecoVenda(peca.getPrecoVenda());
        entidade.setQuantidadeEstoque(peca.getQuantidadeEstoque());
        entidade.setQuantidadeMinima(peca.getQuantidadeMinima());
        entidade.setUnidade(peca.getUnidade());
        entidade.setAtivo(peca.isAtivo());
    }
}
