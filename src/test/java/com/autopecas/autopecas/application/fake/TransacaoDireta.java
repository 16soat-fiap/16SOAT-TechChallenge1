package com.autopecas.autopecas.application.fake;

import com.autopecas.autopecas.application.port.out.Transacao;

import java.util.function.Supplier;

/**
 * Implementação da port Transacao que apenas executa o bloco, sem transação de verdade.
 *
 * <p>É o que permite testar os casos de uso sem Spring: a fronteira transacional é uma port, e
 * no teste ela é substituída por esta execução direta.
 */
public class TransacaoDireta implements Transacao {

    @Override
    public <T> T executar(Supplier<T> operacao) {
        return operacao.get();
    }

    @Override
    public void executar(Runnable operacao) {
        operacao.run();
    }
}
