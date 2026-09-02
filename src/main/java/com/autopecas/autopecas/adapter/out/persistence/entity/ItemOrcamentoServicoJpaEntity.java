package com.autopecas.autopecas.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Entidade JPA do item de serviço do orçamento — filho do agregado Orcamento. */
@Entity
@Table(name = "itens_orcamento_servico")
@Getter
@Setter
@NoArgsConstructor
public class ItemOrcamentoServicoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_orc_serv_orcamento"))
    private OrcamentoJpaEntity orcamento;

    @Column(name = "servico_id", nullable = false)
    private UUID servicoId;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "observacao", length = 300)
    private String observacao;
}
