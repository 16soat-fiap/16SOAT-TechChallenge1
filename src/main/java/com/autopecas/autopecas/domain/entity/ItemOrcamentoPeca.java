package com.autopecas.autopecas.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Item de peça dentro de um Orcamento
 *
 *   - precoUnitario é COPIADO de Peca.precoVenda na inclusão.
 *   - descontoUnitario é o desconto aplicado por unidade.
 *   - Não decrementa estoque — isso só acontece quando o Orcamento é aprovado
 *     e a OS avança para EM_EXECUCAO (implementar no service).
 */
@Entity
@Table(name = "itens_orcamento_peca")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "orcamento")
@EqualsAndHashCode(of = "id")
public class ItemOrcamentoPeca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_orc_peca_orcamento"))
    private Orcamento orcamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "peca_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_orc_peca_catalogo"))
    private Peca peca;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "desconto_unitario", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal descontoUnitario = BigDecimal.ZERO;

    public BigDecimal calcularSubtotalComDesconto() {
        BigDecimal precoComDesconto = precoUnitario.subtract(descontoUnitario);
        return precoComDesconto.multiply(BigDecimal.valueOf(quantidade));
    }
}

