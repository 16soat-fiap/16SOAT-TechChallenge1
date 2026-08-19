package com.autopecas.autopecas.adapter.out.persistence.mapper;

import com.autopecas.autopecas.adapter.out.persistence.entity.EnderecoEmbeddable;
import com.autopecas.autopecas.domain.vo.Endereco;

/** Conversão entre o Value Object Endereco e seu Embeddable de persistência. */
public final class EnderecoJpaMapper {

    private EnderecoJpaMapper() {
    }

    public static Endereco paraDominio(EnderecoEmbeddable entidade) {
        if (entidade == null) {
            return null;
        }
        return new Endereco(entidade.getCep(), entidade.getLogradouro(), entidade.getNumero(),
                entidade.getComplemento(), entidade.getBairro(), entidade.getCidade(),
                entidade.getUf());
    }

    public static EnderecoEmbeddable paraEntidade(Endereco endereco) {
        if (endereco == null) {
            return null;
        }
        return new EnderecoEmbeddable(endereco.cep(), endereco.logradouro(), endereco.numero(),
                endereco.complemento(), endereco.bairro(), endereco.cidade(), endereco.uf());
    }
}
