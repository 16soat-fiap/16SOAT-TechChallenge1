package com.autopecas.autopecas.domain.model.orcamento;

import com.autopecas.autopecas.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Peça proposta dentro de um Orçamento.
 *
 * <p>O precoUnitario é copiado do precoVenda da peça no momento da inclusão. Não há baixa de
 * estoque aqui — ela só ocorre quando o orçamento é aprovado.
 */
public final class ItemOrcamentoPeca {

    private final Long id;
    private final UUID pecaId;
    private final int quantidade;
    private final BigDecimal precoUnitario;

    private ItemOrcamentoPeca(Long id, UUID pecaId, int quantidade, BigDecimal precoUnitario) {
        if (pecaId == null) {
            throw new BusinessException("Item de orçamento deve referenciar uma peça");
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
    }

    /** Novo item, ainda sem id. */
    public static ItemOrcamentoPeca criar(UUID pecaId, int quantidade, BigDecimal precoUnitario) {
        return new ItemOrcamentoPeca(null, pecaId, quantidade, precoUnitario);
    }

    /** Re-hidratação a partir da persistência — uso exclusivo dos mappers de adapter. */
    public static ItemOrcamentoPeca reconstituir(Long id, UUID pecaId, int quantidade,
                                                 BigDecimal precoUnitario) {
        return new ItemOrcamentoPeca(id, pecaId, quantidade, precoUnitario);
    }

    public BigDecimal calcularSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
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
}
