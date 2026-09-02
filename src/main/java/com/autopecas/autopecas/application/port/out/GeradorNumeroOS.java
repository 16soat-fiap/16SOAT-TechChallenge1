package com.autopecas.autopecas.application.port.out;

/**
 * Port de saída para a numeração sequencial das Ordens de Serviço (formato OS-000001).
 *
 * <p>A implementação usa a sequence os_numero_seq do Postgres — detalhe que fica confinado
 * ao adapter de persistência.
 */
public interface GeradorNumeroOS {

    String proximo();
}
