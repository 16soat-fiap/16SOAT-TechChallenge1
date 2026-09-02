package com.autopecas.autopecas.domain.exception;

/** Saldo de estoque insuficiente para uma saída. Traduzida para HTTP 422. */
public class EstoqueInsuficienteException extends BusinessException {
    public EstoqueInsuficienteException(String nomePeca, int disponivel, int solicitado) {
        super(String.format("Estoque insuficiente para '%s'. Disponível: %d, Solicitado: %d",
                nomePeca, disponivel, solicitado));
    }
}
