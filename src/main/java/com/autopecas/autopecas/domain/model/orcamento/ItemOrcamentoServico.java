package com.autopecas.autopecas.domain.model.orcamento;

import com.autopecas.autopecas.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Serviço proposto dentro de um Orçamento.
 *
 * <p>O precoUnitario é copiado do precoBase do serviço no momento da inclusão, preservando o
 * valor histórico mesmo que o catálogo mude depois.
 */
public final class ItemOrcamentoServico {

    private final Long id;
    private final UUID servicoId;
    private final int quantidade;
    private final BigDecimal precoUnitario;
    private final String observacao;

    private ItemOrcamentoServico(Long id, UUID servicoId, int quantidade, BigDecimal precoUnitario,
                                 String observacao) {
        if (servicoId == null) {
            throw new BusinessException("Item de orçamento deve referenciar um serviço");
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
        this.observacao = observacao;
    }

    /** Novo item, ainda sem id. */
    public static ItemOrcamentoServico criar(UUID servicoId, int quantidade, BigDecimal precoUnitario) {
        return new ItemOrcamentoServico(null, servicoId, quantidade, precoUnitario, null);
    }

    /** Re-hidratação a partir da persistência — uso exclusivo dos mappers de adapter. */
    public static ItemOrcamentoServico reconstituir(Long id, UUID servicoId, int quantidade,
                                                    BigDecimal precoUnitario, String observacao) {
        return new ItemOrcamentoServico(id, servicoId, quantidade, precoUnitario, observacao);
    }

    public BigDecimal calcularSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
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

    public String getObservacao() {
        return observacao;
    }
}
