package com.autopecas.autopecas.domain.service;

import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.domain.exception.EstoqueInsuficienteException;
import com.autopecas.autopecas.domain.model.estoque.MovimentacaoEstoque;
import com.autopecas.autopecas.domain.model.estoque.Peca;

import java.util.UUID;

/**
 * Domain service que garante a regra central do estoque: nenhuma alteração de saldo acontece
 * sem o respectivo registro de movimentação.
 *
 * <p>É puro — não persiste nada. Aplica o efeito na Peca e devolve a MovimentacaoEstoque que o
 * caso de uso deve gravar na mesma transação, junto com a peça alterada.
 */
public final class MovimentadorDeEstoque {

    /**
     * Aplica a movimentação no saldo da peça e devolve o registro correspondente.
     *
     * @param peca           peça a ser movimentada (é alterada por este método)
     * @param ordemServicoId OS que originou a saída, ou nulo em movimentações manuais
     * @param executadoPorId funcionário responsável, ou nulo quando a origem é o sistema
     * @throws EstoqueInsuficienteException se for uma saída maior que o saldo disponível
     */
    public MovimentacaoEstoque movimentar(Peca peca, TipoMovimentacaoEstoque tipo, int quantidade,
                                          String motivo, UUID ordemServicoId, UUID executadoPorId) {
        if (tipo == TipoMovimentacaoEstoque.SAIDA) {
            if (!peca.temEstoqueSuficiente(quantidade)) {
                throw new EstoqueInsuficienteException(
                        peca.getNome(), peca.getQuantidadeEstoque(), quantidade);
            }
            peca.decrementarEstoque(quantidade);
        } else {
            peca.incrementarEstoque(quantidade);
        }

        return MovimentacaoEstoque.registrar(peca.getId(), tipo, quantidade,
                peca.getQuantidadeEstoque(), motivo, ordemServicoId, executadoPorId);
    }
}
