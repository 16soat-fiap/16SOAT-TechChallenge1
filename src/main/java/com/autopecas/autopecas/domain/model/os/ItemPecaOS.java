package com.autopecas.autopecas.domain.model.os;

import com.autopecas.autopecas.domain.enums.StatusItemOS;
import com.autopecas.autopecas.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Peça efetivamente consumida em uma Ordem de Serviço.
 *
 * <p>Diferença em relação a ItemOrcamentoPeca: aquele é a peça proposta no orçamento, este é
 * a peça que baixou estoque. O estoque é decrementado quando o orçamento é aprovado, e cada
 * baixa gera uma MovimentacaoEstoque.
 */
public final class ItemPecaOS {

    private final Long id;
    private final UUID pecaId;
    private final int quantidade;
    private final BigDecimal precoUnitario;
    private StatusItemOS status;
    private UUID instaladoPorId;
    private LocalDateTime dataInstalacao;

    private ItemPecaOS(Long id, UUID pecaId, int quantidade, BigDecimal precoUnitario,
                       StatusItemOS status, UUID instaladoPorId, LocalDateTime dataInstalacao) {
        if (pecaId == null) {
            throw new BusinessException("Item de peça deve referenciar uma peça");
        }
        if (quantidade <= 0) {
            throw new BusinessException("Quantidade do item de peça deve ser positiva");
        }
        if (precoUnitario == null) {
            throw new BusinessException("Preço unitário do item de peça é obrigatório");
        }
        this.id = id;
        this.pecaId = pecaId;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.status = status;
        this.instaladoPorId = instaladoPorId;
        this.dataInstalacao = dataInstalacao;
    }

    /** Novo item, ainda sem id, em estado PENDENTE. */
    public static ItemPecaOS criar(UUID pecaId, int quantidade, BigDecimal precoUnitario) {
        return new ItemPecaOS(null, pecaId, quantidade, precoUnitario, StatusItemOS.PENDENTE, null, null);
    }

    /** Re-hidratação a partir da persistência — uso exclusivo dos mappers de adapter. */
    public static ItemPecaOS reconstituir(Long id, UUID pecaId, int quantidade, BigDecimal precoUnitario,
                                          StatusItemOS status, UUID instaladoPorId,
                                          LocalDateTime dataInstalacao) {
        return new ItemPecaOS(id, pecaId, quantidade, precoUnitario, status, instaladoPorId, dataInstalacao);
    }

    public BigDecimal calcularSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public void instalar(UUID mecanicoId, LocalDateTime agora) {
        if (this.status != StatusItemOS.PENDENTE) {
            throw new BusinessException("Apenas itens pendentes podem ser atualizados. Atual: " + status);
        }
        this.status = StatusItemOS.CONCLUIDO;
        this.instaladoPorId = mecanicoId;
        this.dataInstalacao = agora;
    }

    public void cancelar() {
        if (status == StatusItemOS.CONCLUIDO) {
            throw new BusinessException("Status inválido para essa ação. Nesse caso usar Devolução");
        }
        this.status = StatusItemOS.CANCELADO;
    }

    public Long getId() {
        return id;
    }

    public UUID getPecaId() {
        return pecaId;
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

    public UUID getInstaladoPorId() {
        return instaladoPorId;
    }

    public LocalDateTime getDataInstalacao() {
        return dataInstalacao;
    }
}
