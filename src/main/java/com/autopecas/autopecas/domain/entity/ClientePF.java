package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.Genero;
import com.autopecas.autopecas.domain.enums.TipoCliente;
import com.autopecas.autopecas.domain.valueobject.CPF;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Cliente Pessoa Física.
 * Tabela "clientes_pf" — herda campos comuns de "clientes".
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
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class ClientePF extends Cliente {

    @Embedded
    @Column(name = "cpf", nullable = false, updatable = false)
    private CPF cpf;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    // deixei o RG opcional pois ja temos o CPF
    @Column(name = "rg")
    private String rg;

    @Enumerated(EnumType.STRING)
    @Column(name = "genero")
    private Genero genero;

    @Column(name = "profissao")
    private String profissao;

    @Override
    public String getDocumento() {
        return cpf.getValor();
    }

    @Override
    public TipoCliente getTipo() {
        return TipoCliente.PF;
    }
}

