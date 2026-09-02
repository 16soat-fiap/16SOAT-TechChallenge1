package com.autopecas.autopecas.adapter.out.persistence.entity;

import com.autopecas.autopecas.domain.enums.StatusOrcamento;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade JPA do orçamento — raiz de agregado própria.
 *
 * <p>A OS é referenciada apenas pela coluna de FK; os itens são filhos do agregado e
 * permanecem associados com cascade e orphanRemoval.
 */
@Entity
@Table(
        name = "orcamentos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orcamento_os_versao",
                columnNames = {"ordem_servico_id", "versao"}
        ),
        indexes = {
                @Index(name = "idx_orcamento_os", columnList = "ordem_servico_id"),
                @Index(name = "idx_orcamento_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class OrcamentoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "versao", nullable = false)
    private Integer versao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusOrcamento status;

    @Version
    @Column(name = "version_lock", nullable = false)
    private Long versionLock;

    @Column(name = "valor_mao_obra", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMaoObra;

    @Column(name = "valor_pecas", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorPecas;

    @Column(name = "valor_acrescimo", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorAcrescimo;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "condicoes_pagamento", length = 500)
    private String condicoesPagamento;

    @Column(name = "prazo_execucao_dias")
    private Integer prazoExecucaoDias;

    @Column(name = "data_validade")
    private LocalDate dataValidade;

    @Column(name = "observacoes", length = 2000)
    private String observacoes;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    @Column(name = "data_resposta_cliente")
    private LocalDateTime dataRespostaCliente;

    @Column(name = "motivo_rejeicao", length = 500)
    private String motivoRejeicao;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "ordem_servico_id", nullable = false)
    private UUID ordemServicoId;

    @Column(name = "elaborado_por_id")
    private UUID elaboradoPorId;

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ItemOrcamentoServicoJpaEntity> itensServico = new ArrayList<>();

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ItemOrcamentoPecaJpaEntity> itensPeca = new ArrayList<>();

    /** Substitui os itens de serviço preservando a instância da coleção. */
    public void substituirItensServico(List<ItemOrcamentoServicoJpaEntity> novos) {
        this.itensServico.clear();
        novos.forEach(item -> {
            item.setOrcamento(this);
            this.itensServico.add(item);
        });
    }

    /** Substitui os itens de peça preservando a instância da coleção. */
    public void substituirItensPeca(List<ItemOrcamentoPecaJpaEntity> novos) {
        this.itensPeca.clear();
        novos.forEach(item -> {
            item.setOrcamento(this);
            this.itensPeca.add(item);
        });
    }
}
