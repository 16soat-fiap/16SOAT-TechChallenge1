package com.autopecas.autopecas.exception;

public class EstoqueInsuficienteException extends BusinessException{
    public EstoqueInsuficienteException(String nomePeca, int disponivel, int solicitado){
        super(String.format("Estoque insuficiente para '%s'. Disponível: %d, Solicitado: %d",
                nomePeca, disponivel, solicitado));
    }
}
