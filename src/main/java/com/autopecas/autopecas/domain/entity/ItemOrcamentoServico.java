package com.autopecas.autopecas.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Item de serviço dentro de um Orcamento
 *   - precoUnitario de Servico.precoBase na inclusão.
 *   - Mantém o valor histórico mesmo que catálogo mude.
 */
@Entity
@Table(name = "itens_orcamento_servico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "orcamento")
@EqualsAndHashCode(of = "id")
public class ItemOrcamentoServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_orc_serv_orcamento"))
    private Orcamento orcamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_orc_serv_servico"))
    private Servico servico;

    @Column(name = "quantidade", nullable = false)
    @Builder.Default
    private Integer quantidade = 1;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "observacao", length = 300)
    private String observacao;

    public BigDecimal calcularSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
