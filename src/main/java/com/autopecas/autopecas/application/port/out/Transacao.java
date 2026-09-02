package com.autopecas.autopecas.application.port.out;

import java.util.function.Supplier;

/**
 * Port de saída que delimita a fronteira transacional.
 *
 * <p>Os casos de uso não usam a anotação Transactional do Spring — a transação é um detalhe de
 * infraestrutura acessado por esta port. É usada apenas nas operações que precisam de
 * atomicidade entre várias chamadas de repositório; consultas simples não abrem transação.
 */
public interface Transacao {

    <T> T executar(Supplier<T> operacao);

    void executar(Runnable operacao);
}
