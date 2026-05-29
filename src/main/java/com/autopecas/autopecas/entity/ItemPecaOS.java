package com.autopecas.autopecas.entity;

import com.autopecas.autopecas.enums.StatusItemOS;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Item de peça consumido em uma Ordem de Serviço.
 *
 * Diferença vs ItemOrcamentoPeca:
 *   - ItemOrcamentoPeca = peças propostas no orçamento.
 *   - ItemPecaOS = peças que foram usadas (consomem estoque).
 *
 * O estoque é decrementado quando o status do item vai para EM_EXECUCAO,
 * e devolvido se for CANCELADO. Cada movimentação gera registro em
 * Movimentacao Estoque.
 *
 */

@Entity
@Table(name = "itens_peca_os")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ordemServico", "instaladoPor"})
@EqualsAndHashCode(of = "id")
public class ItemPecaOS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_peca_os"))
    private OrdemServico ordemServico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "peca_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_peca_catalogo"))
    private Peca peca;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    //preço da cenda
    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "desconto_unitario", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal descontoUnitario = BigDecimal.ZERO;

    private StatusItemOS stauts = StatusItemOS.PENDENTE;

    //mecanico que executou a atividade
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instalado_por_id",
        foreignKey = @ForeignKey(name="fk_item_peca_mecanico"))
    private Mecanico instaladoPor;

    /**garantia opcional, podemos retirar esse atributo
     * ou criar validações para a garantia
     */
    @Column(name = "garantia_dias", nullable = false)
    @Builder.Default
    private Integer garantiaDias = 90;

    @Column(name = "data_instalacao")
    private LocalDateTime dataInstalacao;

    // Métodos

    public BigDecimal calcularSubtotal(){
        BigDecimal precoComDesconto = precoUnitario.subtract(descontoUnitario);
        return precoComDesconto.multiply(BigDecimal.valueOf(quantidade));
    }

    public void instalar(Mecanico mecanico){
        if(this.stauts != StatusItemOS.PENDENTE){
            throw new IllegalStateException("Apenas itens pendentes podem ser atualizados. Atual: " +stauts);
        }
        this.stauts = StatusItemOS.CONCLUIDO;
        this.instaladoPor = mecanico;
        this.dataInstalacao = LocalDateTime.now();
    }

    public void cancelar(){
        if (stauts == StatusItemOS.CONCLUIDO) {
            throw new IllegalStateException("Status inválido para essa ação. Nesse caso usar Devolução");
        }
        this.stauts = StatusItemOS.CANCELADO;
    }
}
