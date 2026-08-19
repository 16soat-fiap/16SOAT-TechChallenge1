package com.autopecas.autopecas.adapter.out.persistence.entity;

import com.autopecas.autopecas.domain.enums.Genero;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Entidade JPA do cliente PF.
 *
 * <p>O CPF é uma coluna VARCHAR simples: o Value Object CPF vive no domínio e o mapper faz a
 * conversão. Isso elimina o Embeddable anterior sem alterar o schema.
 */
@Entity
@Table(
        name = "clientes_pf",
        uniqueConstraints = @UniqueConstraint(name = "uk_cliente_pf_cpf", columnNames = "cpf")
)
@DiscriminatorValue("PF")
@Getter
@Setter
@NoArgsConstructor
public class ClientePFJpaEntity extends ClienteJpaEntity {

    @Column(name = "cpf", nullable = false, updatable = false, length = 14)
    private String cpf;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "rg")
    private String rg;

    @Enumerated(EnumType.STRING)
    @Column(name = "genero")
    private Genero genero;

    @Column(name = "profissao")
    private String profissao;
}
