package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.domain.enums.StatusOrcamento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 *
 *     Uma OS pode ter MÚLTIPLAS versões de orçamento.
 *   -  atendenteRecepcao quem recebeu o veículo
 *   -  atendenteEntrega quem entregou ao cliente
 *   - valorTotalAprovado é cópia do orçamento aprovado para acesso rápido.
 *
 *   "numero" (ex.: "OS-001") será gerado pela service
 *
 *   RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE
 *   Transições validadas em avancarStatus().
 */
@Entity
@Table(
        name = "ordens_servico",
        uniqueConstraints = @UniqueConstraint(name = "uk_os_numero", columnNames = "numero"),
        indexes = {
                @Index(name = "idx_os_cliente",  columnList = "cliente_id"),
                @Index(name = "idx_os_veiculo",  columnList = "veiculo_id"),
                @Index(name = "idx_os_status",   columnList = "status"),
                @Index(name = "idx_os_mecanico", columnList = "mecanico_responsavel_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {
        "cliente", "veiculo", "mecanicoResponsavel",
        "atendenteRecepcao", "atendenteEntrega",
        "itensServico", "itensPeca", "orcamentos", "historico"
})
@EqualsAndHashCode(of = "id")
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "numero", nullable = false, updatable = false)
    private String numero;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private StatusOS status = StatusOS.RECEBIDA;

    @Column(name = "quilometragem_entrada")
    private Integer quilometragemEntrada;

    @Column(name = "observacoes_entrada", length = 1000)
    private String observacoesEntrada;

    @Column(name = "diagnostico", length = 2000)
    private String diagnostico;

    /** Reclamação inicial relatada pelo cliente. */
    @Column(name = "queixa_cliente", length = 1000)
    private String queixaCliente;

    /**
     * Valor total do orçamento aprovado (cópia para acesso rápido).
     * Atualizado quando um Orcamento é aprovado.
     * Mantém-se zero enquanto nenhum orçamento foi aprovado.
     */
    @Column(name = "valor_total_aprovado", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorTotalAprovado = BigDecimal.ZERO;

    // DATAS ---------------------------------------------

    @Column(name = "data_inicio_execucao")
    private LocalDateTime dataInicioExecucao;

    @Column(name = "data_finalizacao")
    private LocalDateTime dataFinalizacao;

    @Column(name = "data_entrega")
    private LocalDateTime dataEntrega;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // RELACIONAMENTOS -------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_os_cliente"))
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_os_veiculo"))
    private Veiculo veiculo;

    /** Atendente que recepcionou o veículo na entrada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atendente_recepcao_id", foreignKey = @ForeignKey(name = "fk_os_atendente_recepcao"))
    private Atendente atendenteRecepcao;

    /** Atendente que entregou o veículo ao cliente. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atendente_entrega_id", foreignKey = @ForeignKey(name = "fk_os_atendente_entrega"))
    private Atendente atendenteEntrega;

    /** Mecânico responsável pela execução. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mecanico_responsavel_id", foreignKey = @ForeignKey(name = "fk_os_mecanico"))
    private Mecanico mecanicoResponsavel;

    /**
     * Itens efetivamente executados nesta OS.
     * Itens da OS são o que está ou será EXECUTADO orçamento aprovado.
     */
    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemServicoOS> itensServico = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemPecaOS> itensPeca = new ArrayList<>();

    /** Versões de orçamento para esta OS (1, 2, 3...). */
    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Orcamento> orcamentos = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<HistoricoStatusOS> historico = new ArrayList<>();

    // ─── Métodos de domínio ─────────────────────────────────────────────────────

    private static final java.util.Map<StatusOS, Set<StatusOS>> TRANSICOES_PERMITIDAS = java.util.Map.of(
            StatusOS.RECEBIDA,             Set.of(StatusOS.EM_DIAGNOSTICO),
            StatusOS.EM_DIAGNOSTICO,       Set.of(StatusOS.AGUARDANDO_APROVACAO),
            StatusOS.AGUARDANDO_APROVACAO, Set.of(StatusOS.EM_EXECUCAO, StatusOS.EM_DIAGNOSTICO),
            StatusOS.EM_EXECUCAO,          Set.of(StatusOS.FINALIZADA),
            StatusOS.FINALIZADA,           Set.of(StatusOS.ENTREGUE),
            StatusOS.ENTREGUE,             Set.of()
    );

    /** Valida e executa a transição de status. Lança IllegalStateException se inválida. */
    public void avancarStatus(StatusOS novoStatus) {
        Set<StatusOS> permitidos = TRANSICOES_PERMITIDAS.getOrDefault(this.status, Set.of());
        if (!permitidos.contains(novoStatus)) {
            throw new IllegalStateException(
                    String.format("Transição de status inválida: %s → %s. Permitidos a partir de %s: %s",
                            this.status, novoStatus, this.status, permitidos)
            );
        }

        switch (novoStatus) {
            case EM_EXECUCAO -> this.dataInicioExecucao = LocalDateTime.now();
            case FINALIZADA  -> this.dataFinalizacao    = LocalDateTime.now();
            case ENTREGUE    -> this.dataEntrega        = LocalDateTime.now();
            default -> { /* sem efeito colateral */ }
        }
        this.status = novoStatus;
    }

    /** Retorna o orçamento aprovado */
    public Optional<Orcamento> getOrcamentoAprovado() {
        return orcamentos.stream()
                .filter(o -> o.getStatus() == StatusOrcamento.APROVADO)
                .findFirst();
    }

    /** Retorna a última versão de orçamento (independente do status). */
    public Optional<Orcamento> getOrcamentoVigente() {
        return orcamentos.stream()
                .max(java.util.Comparator.comparing(Orcamento::getVersao));
    }

    /** Adiciona um item de serviço (executado) à OS. */
    public void adicionarItemServico(ItemServicoOS item) {
        item.setOrdemServico(this);
        this.itensServico.add(item);
    }

    /** Adiciona um item de peça (executado) à OS. */
    public void adicionarItemPeca(ItemPecaOS item) {
        item.setOrdemServico(this);
        this.itensPeca.add(item);
    }

    /** Adiciona uma nova versão de orçamento. A versão é calculada automaticamente. */
    public void adicionarOrcamento(Orcamento orcamento) {
        int proximaVersao = orcamentos.stream()
                .mapToInt(Orcamento::getVersao)
                .max()
                .orElse(0) + 1;
        orcamento.setVersao(proximaVersao);
        orcamento.setOrdemServico(this);
        this.orcamentos.add(orcamento);
    }

    /** Tempo total de execução em minutos (FINALIZADA - INICIO_EXECUCAO). */
    public Long calcularTempoExecucaoMinutos() {
        if (dataInicioExecucao == null || dataFinalizacao == null) {
            return null;
        }
        return java.time.Duration.between(dataInicioExecucao, dataFinalizacao).toMinutes();
    }
}
