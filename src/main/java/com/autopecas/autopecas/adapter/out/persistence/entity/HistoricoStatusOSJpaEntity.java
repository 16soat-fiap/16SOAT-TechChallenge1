package com.autopecas.autopecas.adapter.out.persistence.entity;

import com.autopecas.autopecas.domain.enums.StatusOS;
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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA do histórico de status. Somente-inserção e independente da OS: a OS é
 * referenciada apenas pela coluna de FK.
 */
@Entity
@Table(
        name = "historico_status_os",
        indexes = {
                @Index(name = "idx_historico_os", columnList = "ordem_servico_id"),
                @Index(name = "idx_historico_executor", columnList = "executado_por_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class HistoricoStatusOSJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "ordem_servico_id", nullable = false)
    private UUID ordemServicoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior")
    private StatusOS statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false)
    private StatusOS statusNovo;

    @Column(name = "observacao", length = 500)
    private String observacao;

    @Column(name = "alterado_por", nullable = false)
    private String alteradoPor;

    @Column(name = "executado_por_id")
    private UUID executadoPorId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
