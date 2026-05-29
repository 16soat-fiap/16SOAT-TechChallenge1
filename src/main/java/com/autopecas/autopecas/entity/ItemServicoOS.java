package com.autopecas.autopecas.entity;

import com.autopecas.autopecas.enums.StatusItemOS;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Item de serviço executados em uma Ordem de Serviço.
 *
 * Diferença do ItemOrcamentoServico:
 *   - ItemOrcamentoServico = o que foi pensando no orçamento.
 *   - ItemServicoOS  = o que está ou será EXECUTADO.
 *
 * O service copia os itens do orçamento aprovado para a OS quando
 * o status avança para EM_EXECUCAO.
 *
 * Rastreabilidade:
 *   - executadoPor: mecânico que efetivamente fez o serviço (pode diferir
 *     do mecanicoResponsavel da OS — em OS grandes, vários mecânicos atuam).
 *   - dataInicio/dataFim: tempo real de execução por item (vs tempo estimado
 *     em Servico.tempoEstimadoMinutos — base para análise de produtividade).
 */
@Entity
@Table(name = "itens_servico_os")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ordemServico", "executadoPor"})
@EqualsAndHashCode(of = "id")
public class ItemServicoOS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_servico_os"))
    private OrdemServico ordemServico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_servico_catalogo"))
    private Servico servico;

    @Column(name = "quantidade", nullable = false)
    @Builder.Default
    private Integer quantidade = 1;

    /** Snapshot do preço no momento da execução. */
    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    /** Desconto por unidade (já aplicado no orçamento aprovado). */
    @Column(name = "desconto_unitario", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal descontoUnitario = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private StatusItemOS status = StatusItemOS.PENDENTE;

    /** Mecânico que efetivamente executou este serviço (pode ser diferente do responsável da OS). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executado_por_id",
            foreignKey = @ForeignKey(name = "fk_item_servico_mecanico"))
    private Mecanico executadoPor;

    @Column(name = "data_inicio_execucao")
    private LocalDateTime dataInicioExecucao;

    @Column(name = "data_fim_execucao")
    private LocalDateTime dataFimExecucao;

    /** OPCIONAL podemos retirara garantia ou criar validações
     * com ela */
    @Column(name = "garantia_dias", nullable = false)
    @Builder.Default
    private Integer garantiaDias = 90;

    @Column(name = "observacao", length = 500)
    private String observacao;

    // Métodos ---------------------------------------

    public BigDecimal calcularSubtotal(){
        BigDecimal precoComDesconto = precoUnitario.subtract(descontoUnitario);
        return precoComDesconto.multiply(BigDecimal.valueOf(quantidade));
    }

    public void iniciarExecucao(Mecanico mecanico) {
        if (status != StatusItemOS.PENDENTE) {
            throw new IllegalStateException("Apenas itens pendentes podem ser alterados. Atual: " + status);
        }
        this.status = StatusItemOS.EM_EXECUCAO;
        this.dataInicioExecucao = LocalDateTime.now();
        this.executadoPor = mecanico;
    }

    public void concluir() {
        if (status != StatusItemOS.EM_EXECUCAO) {
            throw new IllegalStateException("Apenas itens EM_EXECUCAO podem ser concluídos. Atual: " + status);
        }
        this.status = StatusItemOS.CONCLUIDO;
        this.dataFimExecucao = LocalDateTime.now();
    }

    public void cancelar(){
        if(status == StatusItemOS.CONCLUIDO){
            throw new IllegalStateException("Não é possível cancelar itens concluídos");
        }
        this.status = StatusItemOS.CANCELADO;
    }

    //TEMPO
    public Long calcularTempoDeExecucaoMinutos(){
        if(dataInicioExecucao == null || dataFimExecucao == null) return null;
        return java.time.Duration.between(dataInicioExecucao, dataFimExecucao).toMinutes();
    }
}

