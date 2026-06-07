package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de qualquer movimentação no estoque de uma peça.
 *
 * Regras:
 *   - Registros não são alterados nem deletados.
 *   - quantidade é SEMPRE positiva — o "sentido" vem de "tipo".
 *   - quantidadeEstoque na Peca é o saldo derivado da soma das movimentações,
 *     mantido em cache para performance (atualizado dentro da mesma transação).
 *   - ordemServico é preenchida em movimentações tipo SAIDA por uso em OS.
 *   - valorUnitarioMomento captura o custo no momento da entrada (FIFO/médio).
 */
@Entity
@Table(
        name = "movimentacoes_estoque",
        indexes = {
                @Index(name = "idx_movimentacao_peca",    columnList = "peca_id"),
                @Index(name = "idx_movimentacao_data",    columnList = "created_at"),
                @Index(name = "idx_movimentacao_os",      columnList = "ordem_servico_id"),
                @Index(name = "idx_movimentacao_tipo",    columnList = "tipo")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"peca", "ordemServico", "executadoPor"})
@EqualsAndHashCode(of = "id")
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "peca_id", nullable = false, foreignKey = @ForeignKey(name = "fk_movimentacao_peca"))
    private Peca peca;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoMovimentacaoEstoque tipo;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    /** Saldo da peça APÓS aplicar esta movimentação*/
    @Column(name = "saldo_apos", nullable = false)
    private Integer saldoApos;

    /** Custo unitário no momento da movimentação */
    @Column(name = "valor_unitario_momento", precision = 10, scale = 2)
    private BigDecimal valorUnitarioMomento;

    @Column(name = "motivo", length = 300)
    private String motivo;

    // CRIAR UM ATRIBUTO PARA NF ???

    /** OS associada — preenchida em SAIDA/DEVOLUCAO. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id", foreignKey = @ForeignKey(name = "fk_movimentacao_os"))
    private OrdemServico ordemServico;

    /** Funcionário que registrou a movimentação. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executado_por_id", foreignKey = @ForeignKey(name = "fk_movimentacao_funcionario"))
    private Funcionario executadoPor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

