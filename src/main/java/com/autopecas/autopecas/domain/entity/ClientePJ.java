package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.TipoCliente;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Cliente Pessoa Jurídica.
 * Tabela "clientes_pj" — herda campos comuns de "clientes".
 */


@Entity
@Table(
        name = "clientes_pj",
        uniqueConstraints = @UniqueConstraint(name = "uk_cliente_pj_cnpj", columnNames = "cnpj")
)
@DiscriminatorValue("PJ")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class ClientePJ extends Cliente {

    @Column(name = "cnpj", nullable = false, updatable = false)
    private String cnpj;

    @Column(name = "razao_social", nullable = false)
    private String razaoSocial;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    // Mantive como string, pois pode ser ISENTO
    @Column(name = "inscricao_estadual")
    private String inscricaoEstadual;

    // campos opcionais
    @Column(name = "inscricao_municipal")
    private String inscricaoMunicipal;

    //
    @Column(name = "contato_responsavel")
    private String contatoResponsavel;

    @Override
    public String getDocumento() {
        return cnpj;
    }

    @Override
    public TipoCliente getTipo() {
        return TipoCliente.PJ;
    }
}
