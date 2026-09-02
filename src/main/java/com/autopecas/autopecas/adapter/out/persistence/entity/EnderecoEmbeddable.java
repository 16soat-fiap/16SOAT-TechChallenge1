package com.autopecas.autopecas.adapter.out.persistence.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contrapartida persistente do Value Object Endereco.
 *
 * <p>As colunas concretas são definidas por AttributeOverrides em cada entidade que embute o
 * endereço, porque clientes e funcionarios usam tamanhos diferentes.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnderecoEmbeddable {

    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
}
