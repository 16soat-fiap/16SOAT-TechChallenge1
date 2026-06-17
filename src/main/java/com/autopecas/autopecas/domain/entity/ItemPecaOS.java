package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.StatusItemOS;
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
 * O estoque é decrementado quando o orçamento é aprovado.
 * Cada movimentação gera registro em MovimentacaoEstoque.
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

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private StatusItemOS status = StatusItemOS.PENDENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instalado_por_id",
        foreignKey = @ForeignKey(name = "fk_item_peca_mecanico"))
    private Mecanico instaladoPor;

    @Column(name = "data_instalacao")
    private LocalDateTime dataInstalacao;

    // Métodos

    public BigDecimal calcularSubtotal(){
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public void instalar(Mecanico mecanico){
        if(this.status != StatusItemOS.PENDENTE){
            throw new IllegalStateException("Apenas itens pendentes podem ser atualizados. Atual: " + status);
        }
        this.status = StatusItemOS.CONCLUIDO;
        this.instaladoPor = mecanico;
        this.dataInstalacao = LocalDateTime.now();
    }

    public void cancelar(){
        if (status == StatusItemOS.CONCLUIDO) {
            throw new IllegalStateException("Status inválido para essa ação. Nesse caso usar Devolução");
        }
        this.status = StatusItemOS.CANCELADO;
    }
}
