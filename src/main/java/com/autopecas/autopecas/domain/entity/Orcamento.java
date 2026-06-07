package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.StatusOrcamento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orçamento de uma Ordem de Serviço.
 * <p>
 * Modelagem:
 * - Uma Ordem pode ter MÚLTIPLOS orçamentos por isso o versionamento.
 * - Cada orçamento é um SNAPSHOT dos itens (serviços + peças) no momento.
 * - Se cliente rejeitar, criamos um orçamento (versao = versao + 1)
 * em vez de editar — preserva histórico de negociação.
 * Ciclo de vida (StatusOrcamento):
 * RASCUNHO → ENVIADO → APROVADO | REJEITADO | EXPIRADO | CANCELADO
 * Regras:
 * - Apenas UM orçamento por OS pode estar APROVADO.
 * - Aprovar um orçamento dispara o avanço OS para EM_EXECUCAO.
 * - Rejeitar permite criar versão (com motivo).
 * - data Validade ultrapassada com status ENVIADO marca como EXPIRADO.
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
@AllArgsConstructor
@Builder
@ToString(exclude = {"ordemServico", "itensServico", "itensPeca", "elaboradoPor", "aprovadoPorGestor"})
@EqualsAndHashCode(of = "id")
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    //versão da OS
    @Column(name = "versao", nullable = false)
    private Integer versao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StatusOrcamento status = StatusOrcamento.RASCUNHO;

    @Version
    @Column(name = "version_lock", nullable = false)
    private Long versionLock;

    // Valores (snapshot) ─────────────────────────────────────────────────────

    @Column(name = "valor_mao_obra", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorMaoObra = BigDecimal.ZERO;

    @Column(name = "valor_pecas", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorPecas = BigDecimal.ZERO;

    @Column(name = "valor_desconto", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorDesconto = BigDecimal.ZERO;

    @Column(name = "valor_acrescimo", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorAcrescimo = BigDecimal.ZERO;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    // Condições pagamento  ───────────────────────────────────────────────────

    @Column(name = "condicoes_pagamento", length = 500)
    private String condicoesPagamento;

    @Column(name = "prazo_execucao_dias")
    private Integer prazoExecucaoDias;

    /**
     * Data até a qual o orçamento é válido. Após isso, vira EXPIRADO.
     */
    @Column(name = "data_validade")
    private LocalDate dataValidade;

    @Column(name = "observacoes", length = 2000)
    private String observacoes;

    // Datas relativas a OS ─────────────────────────────────────────────────────────

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

    // ─── Relacionamentos ────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_orcamento_os"))
    private OrdemServico ordemServico;

    /**
     * Atendente que montou o orçamento.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elaborado_por_id",
            foreignKey = @ForeignKey(name = "fk_orcamento_elaborador"))
    private Atendente elaboradoPor;

    // Gestor que aprovou desconto/orçamento (quando atendente não tem autonomia para o valor).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovado_por_gestor_id",
            foreignKey = @ForeignKey(name = "fk_orcamento_gestor"))
    private Gestor aprovadoPorGestor;

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemOrcamentoServico> itensServico = new ArrayList<>();

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemOrcamentoPeca> itensPeca = new ArrayList<>();

    // ─── Métodos de domínio ─────────────────────────────────────────────────────

    //recalcula
    public void recalcular() {
        this.valorMaoObra = itensServico.stream()
                .map(ItemOrcamentoServico::calcularSubtotalComDesconto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.valorPecas = itensPeca.stream()
                .map(ItemOrcamentoPeca::calcularSubtotalComDesconto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.valorTotal = valorMaoObra
                .add(valorPecas)
                .subtract(valorDesconto)
                .add(valorAcrescimo);
    }

    /** marca o enviado e registra a data */
    public void enviar(){
        if (this.status != StatusOrcamento.RASCUNHO){
            throw new IllegalStateException("Apenas orçamentos em Rascunho podem ser enviados. Atual: " + status);
        }
        this.status = StatusOrcamento.ENVIADO;
        this.dataEnvio = LocalDateTime.now();
    }

    // aprovação do cliente
    public void aprovar(){
        if (this.status != StatusOrcamento.ENVIADO){
            throw new IllegalStateException("Apenas orçamentos enviados podem ser aprovados. Atual" + status);
        }
        this.status = StatusOrcamento.APROVADO;
        this.dataRespostaCliente = LocalDateTime.now();
    }

    //rejeição do cliente
    public void rejeitar(String motivo){
        if (this.status != StatusOrcamento.ENVIADO){
            throw new IllegalStateException("Apenas orçamentos enviados podem ser rejeitados. Atual:" + status);
        }
        this.status = StatusOrcamento.REJEITADO;
        this.dataRespostaCliente = LocalDateTime.now();
        this.motivoRejeicao = motivo;
    }

    //verifica a data de validade do orcamento
    public boolean estaExpirado(){
        return dataValidade != null && dataValidade.isBefore(LocalDate.now());
    }

    /**verifica se o orçamento ainda é valido
     * podemos usar para o retorno do cliente
    */
    public boolean estaValido(){
        return status == StatusOrcamento.ENVIADO && !estaExpirado();
    }

    //Marca o orçamento como expirado
    public void expirar(){
        if(this.status != StatusOrcamento.ENVIADO) return;
        if(!estaExpirado()) return;
        this.status = StatusOrcamento.EXPIRADO;
    }


}
