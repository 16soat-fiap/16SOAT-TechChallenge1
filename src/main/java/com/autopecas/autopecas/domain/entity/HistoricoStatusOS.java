package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.StatusOS;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Registro de cada transição de status de uma OS
 *
 * Rastreabilidade
 *- alteradoPor : string
 *- executadoPor: Funcionario (Mecanico/Atendente)
 *
 * Regras:
 *- Registros não são alterados nem deletados.
 *- Toda transição via OrdemServico.avancarStatus() deve gerar um novo registro.
 */
@Entity
@Table(
        name = "historico_status_os",
        indexes = {
                @Index(name = "idx_historico_os",      columnList = "ordem_servico_id"),
                @Index(name = "idx_historico_executor", columnList = "executado_por_id")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ordemServico", "executadoPor"})
@EqualsAndHashCode(of = "id")
public class HistoricoStatusOS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_historico_os"))
    private OrdemServico ordemServico;

    // os primeiros registros vão estar null - criação da OS — sem status anterior
    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior")
    private StatusOS statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false)
    private StatusOS statusNovo;

    @Column(name = "observacao", length = 500)
    private String observacao;

    /**
     * Nome legível de quem realizou a mudança.
     * Ex.: "Carlos Silva (FUN-0042) — Mecânico, SISTEMA.
     */
    @Column(name = "alterado_por", nullable = false)
    private String alteradoPor;

    /**
     * Funcionário que executou a mudança (Mecanico ou Atendente).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executado_por_id",
            foreignKey = @ForeignKey(name = "fk_historico_funcionario"))
    private Funcionario executadoPor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ─── Factory methods ─────────────────────────────────────────────────────────

    /** Registro de abertura da OS (sem status anterior). */
    public static HistoricoStatusOS abertura(OrdemServico os, Funcionario atendente) {
        return HistoricoStatusOS.builder()
                .ordemServico(os)
                .statusAnterior(null)
                .statusNovo(StatusOS.RECEBIDA)
                .observacao("Ordem de serviço aberta.")
                .alteradoPor(atendente.getIdentificacao() + " — " + atendente.getTipo())
                .executadoPor(atendente)
                .build();
    }

    /** Registro de transição executada por um funcionário identificado. */
    public static HistoricoStatusOS porFuncionario(OrdemServico os,
                                                   StatusOS anterior,
                                                   StatusOS novo,
                                                   String observacao,
                                                   Funcionario funcionario) {
        return HistoricoStatusOS.builder()
                .ordemServico(os)
                .statusAnterior(anterior)
                .statusNovo(novo)
                .observacao(observacao)
                .alteradoPor(funcionario.getIdentificacao() + " — " + funcionario.getTipo())
                .executadoPor(funcionario)
                .build();
    }

    /** Registro de transição automática feita pelo sistema. */
    public static HistoricoStatusOS porSistema(OrdemServico os,
                                               StatusOS anterior,
                                               StatusOS novo,
                                               String observacao) {
        return HistoricoStatusOS.builder()
                .ordemServico(os)
                .statusAnterior(anterior)
                .statusNovo(novo)
                .observacao(observacao)
                .alteradoPor("SISTEMA")
                .executadoPor(null)
                .build();
    }
}

