package com.autopecas.autopecas.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entidade JPA do cliente PJ. O CNPJ é coluna VARCHAR simples. */
@Entity
@Table(
        name = "clientes_pj",
        uniqueConstraints = @UniqueConstraint(name = "uk_cliente_pj_cnpj", columnNames = "cnpj")
)
@DiscriminatorValue("PJ")
@Getter
@Setter
@NoArgsConstructor
public class ClientePJJpaEntity extends ClienteJpaEntity {

    @Column(name = "cnpj", nullable = false, updatable = false, length = 18)
    private String cnpj;

    @Column(name = "razao_social", nullable = false)
    private String razaoSocial;

    @Column(name = "inscricao_estadual")
    private String inscricaoEstadual;

    @Column(name = "inscricao_municipal")
    private String inscricaoMunicipal;

    @Column(name = "contato_responsavel")
    private String contatoResponsavel;
}
