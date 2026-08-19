package com.autopecas.autopecas.adapter.out.tx;

import com.autopecas.autopecas.application.port.out.Transacao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Implementação da port Transacao com TransactionTemplate.
 *
 * <p>É aqui que a transação vive: os casos de uso não conhecem a anotação Transactional do
 * Spring, apenas pedem que um bloco execute atomicamente.
 */
@Component
public class TransacaoSpring implements Transacao {

    private final TransactionTemplate transactionTemplate;

    public TransacaoSpring(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T executar(Supplier<T> operacao) {
        return transactionTemplate.execute(status -> operacao.get());
    }

    @Override
    public void executar(Runnable operacao) {
        transactionTemplate.executeWithoutResult(status -> operacao.run());
    }
}
