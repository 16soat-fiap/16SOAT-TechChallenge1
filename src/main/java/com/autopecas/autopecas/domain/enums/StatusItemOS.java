package com.autopecas.autopecas.domain.enums;

/**
 * Status de execução de um item (serviço ou peça) dentro de uma OS.
 *
 * PENDENTE — incluído no orçamento, ainda não iniciado.
 * EM_EXECUCAO — mecânico iniciou a execução deste item.
 * CONCLUIDO — execução finalizada.
 * CANCELADO — item removido do escopo da OS (não será executado).
 */
public enum StatusItemOS {
    PENDENTE,
    EM_EXECUCAO,
    CONCLUIDO,
    CANCELADO
}
