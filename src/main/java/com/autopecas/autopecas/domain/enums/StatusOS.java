package com.autopecas.autopecas.domain.enums;

/**
 * Ciclo de vida de uma Ordem de Serviço.
 *
 * Fluxo esperado:
 * RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE
 *
 * Transições permitidas:
 *   RECEBIDA              → EM_DIAGNOSTICO
 *   EM_DIAGNOSTICO        → AGUARDANDO_APROVACAO
 *   AGUARDANDO_APROVACAO  → EM_EXECUCAO  (cliente aprovou)
 *   AGUARDANDO_APROVACAO  → RECEBIDA     (cliente rejeitou — revisão do orçamento)
 *   EM_EXECUCAO           → FINALIZADA
 *   FINALIZADA            → ENTREGUE
 */
public enum StatusOS {

    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE
}
