package com.autopecas.autopecas.domain.model.os;

import com.autopecas.autopecas.domain.enums.StatusItemOS;
import com.autopecas.autopecas.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Serviço efetivamente executado em uma Ordem de Serviço.
 *
 * <p>Diferença em relação a ItemOrcamentoServico: aquele é o que foi proposto no orçamento,
 * este é o que está ou será executado. Os itens são copiados do orçamento aprovado.
 *
 * <p>O mecânico que executa pode diferir do responsável pela OS — em OS grandes, vários
 * mecânicos atuam.
 */
public final class ItemServicoOS {

    private final Long id;
    private final UUID servicoId;
    private final int quantidade;
    private final BigDecimal precoUnitario;
    private StatusItemOS status;
    private UUID executadoPorId;
    private LocalDateTime dataInicioExecucao;
    private LocalDateTime dataFimExecucao;
    private String observacao;

    private ItemServicoOS(Long id, UUID servicoId, int quantidade, BigDecimal precoUnitario,
                          StatusItemOS status, UUID executadoPorId, LocalDateTime dataInicioExecucao,
                          LocalDateTime dataFimExecucao, String observacao) {
        if (servicoId == null) {
            throw new BusinessException("Item de serviço deve referenciar um serviço");
        }
        if (quantidade <= 0) {
            throw new BusinessException("Quantidade do item de serviço deve ser positiva");
        }
        if (precoUnitario == null) {
            throw new BusinessException("Preço unitário do item de serviço é obrigatório");
        }
        this.id = id;
        this.servicoId = servicoId;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.status = status;
        this.executadoPorId = executadoPorId;
        this.dataInicioExecucao = dataInicioExecucao;
        this.dataFimExecucao = dataFimExecucao;
        this.observacao = observacao;
    }

    /** Novo item, ainda sem id, em estado PENDENTE. */
    public static ItemServicoOS criar(UUID servicoId, int quantidade, BigDecimal precoUnitario) {
        return new ItemServicoOS(null, servicoId, quantidade, precoUnitario, StatusItemOS.PENDENTE,
                null, null, null, null);
    }

    /** Re-hidratação a partir da persistência — uso exclusivo dos mappers de adapter. */
    public static ItemServicoOS reconstituir(Long id, UUID servicoId, int quantidade,
                                             BigDecimal precoUnitario, StatusItemOS status,
                                             UUID executadoPorId, LocalDateTime dataInicioExecucao,
                                             LocalDateTime dataFimExecucao, String observacao) {
        return new ItemServicoOS(id, servicoId, quantidade, precoUnitario, status, executadoPorId,
                dataInicioExecucao, dataFimExecucao, observacao);
    }

    public BigDecimal calcularSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public void iniciarExecucao(UUID mecanicoId, LocalDateTime agora) {
        if (status != StatusItemOS.PENDENTE) {
            throw new BusinessException("Apenas itens pendentes podem ser alterados. Atual: " + status);
        }
        this.status = StatusItemOS.EM_EXECUCAO;
        this.dataInicioExecucao = agora;
        this.executadoPorId = mecanicoId;
    }

    public void concluir(LocalDateTime agora) {
        if (status != StatusItemOS.EM_EXECUCAO) {
            throw new BusinessException("Apenas itens EM_EXECUCAO podem ser concluídos. Atual: " + status);
        }
        this.status = StatusItemOS.CONCLUIDO;
        this.dataFimExecucao = agora;
    }

    public void cancelar() {
        if (status == StatusItemOS.CONCLUIDO) {
            throw new BusinessException("Não é possível cancelar itens concluídos");
        }
        this.status = StatusItemOS.CANCELADO;
    }

    /** Tempo real de execução em minutos, ou nulo se ainda não concluído. */
    public Long calcularTempoDeExecucaoMinutos() {
        if (dataInicioExecucao == null || dataFimExecucao == null) {
            return null;
        }
        return Duration.between(dataInicioExecucao, dataFimExecucao).toMinutes();
    }

    public Long getId() {
        return id;
    }

    public UUID getServicoId() {
        return servicoId;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public StatusItemOS getStatus() {
        return status;
    }

    public UUID getExecutadoPorId() {
        return executadoPorId;
    }

    public LocalDateTime getDataInicioExecucao() {
        return dataInicioExecucao;
    }

    public LocalDateTime getDataFimExecucao() {
        return dataFimExecucao;
    }

    public String getObservacao() {
        return observacao;
    }
}
