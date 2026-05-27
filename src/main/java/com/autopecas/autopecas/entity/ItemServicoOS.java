package com.autopecas.autopecas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "itens_servico_os")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ordemServico")
@EqualsAndHashCode(of = "id")
public class ItemServicoOS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false, foreignKey = @ForeignKey(name = "fk_item_servico_os"))
    private OrdemServico ordemServico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false, foreignKey = @ForeignKey(name = "fk_item_servico_catalogo"))
    private Servico servico;

    @Column(name = "quantidade", nullable = false)
    @Builder.Default
    private Integer quantidade = 1;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "observacao", length = 300)
    private String observacao;

}

