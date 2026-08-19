package com.autopecas.autopecas.adapter.out.persistence.mapper;

import com.autopecas.autopecas.adapter.out.persistence.entity.ServicoJpaEntity;
import com.autopecas.autopecas.domain.model.estoque.Servico;
import org.springframework.stereotype.Component;

/** Conversão entre o agregado Servico e sua entidade JPA. */
@Component
public class ServicoJpaMapper {

    public Servico paraDominio(ServicoJpaEntity entidade) {
        return Servico.reconstituir(entidade.getId(), entidade.getNome(), entidade.getDescricao(),
                entidade.getPrecoBase(), entidade.getTempoEstimadoMinutos(),
                Boolean.TRUE.equals(entidade.getAtivo()));
    }

    /** Cria a entidade de um serviço ainda não persistido. */
    public ServicoJpaEntity novaEntidade(Servico servico) {
        ServicoJpaEntity entidade = new ServicoJpaEntity();
        aplicar(servico, entidade);
        return entidade;
    }

    /** Aplica o estado do agregado sobre uma entidade já carregada. */
    public void aplicar(Servico servico, ServicoJpaEntity entidade) {
        entidade.setNome(servico.getNome());
        entidade.setDescricao(servico.getDescricao());
        entidade.setPrecoBase(servico.getPrecoBase());
        entidade.setTempoEstimadoMinutos(servico.getTempoEstimadoMinutos());
        entidade.setAtivo(servico.isAtivo());
    }
}
