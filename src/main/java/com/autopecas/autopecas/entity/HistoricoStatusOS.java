package com.autopecas.autopecas.entity;

import com.autopecas.autopecas.enums.StatusOS;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_status_os",
        indexes = @Index(name = "idx_historico_os_id", columnList = "ordem_servico_id")
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ordemServico")
@EqualsAndHashCode(of = "id")
public class HistoricoStatusOS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false, foreignKey = @ForeignKey(name = "fk_historico_os"))
    private OrdemServico ordemServico;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
