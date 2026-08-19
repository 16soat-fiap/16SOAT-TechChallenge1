package com.autopecas.autopecas.adapter.out.persistence.entity;

import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Entidade JPA da movimentação de estoque. Somente-inserção. */
@Entity
@Table(
        name = "movimentacoes_estoque",
        indexes = {
                @Index(name = "idx_movimentacao_peca", columnList = "peca_id"),
                @Index(name = "idx_movimentacao_data", columnList = "created_at"),
                @Index(name = "idx_movimentacao_os", columnList = "ordem_servico_id"),
                @Index(name = "idx_movimentacao_tipo", columnList = "tipo")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class MovimentacaoEstoqueJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "peca_id", nullable = false)
    private UUID pecaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoMovimentacaoEstoque tipo;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "saldo_apos", nullable = false)
    private Integer saldoApos;

    @Column(name = "valor_unitario_momento", precision = 10, scale = 2)
    private BigDecimal valorUnitarioMomento;

    @Column(name = "motivo", length = 300)
    private String motivo;

    @Column(name = "ordem_servico_id")
    private UUID ordemServicoId;

    @Column(name = "executado_por_id")
    private UUID executadoPorId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
