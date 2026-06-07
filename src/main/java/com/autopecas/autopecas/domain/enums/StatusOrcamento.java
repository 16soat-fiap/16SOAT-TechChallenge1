package com.autopecas.autopecas.domain.enums;

/**
 * Ciclo de vida de um Orcamento associado a uma OS.
 *
 * RASCUNHO   — sendo montado pelo atendente, ainda não enviado. ( ??? )
 * ENVIADO    — enviado ao cliente, aguardando resposta.
 * APROVADO   — cliente aprovou; a OS pode avançar para EM_EXECUCAO.
 * REJEITADO  — cliente rejeitou; novo orçamento pode ser criado (nova versão).
 * EXPIRADO   — dataValidade ultrapassada sem resposta.
 * CANCELADO  — cancelado internamente (ex.: substituído por nova versão).
 */
public enum StatusOrcamento {
    RASCUNHO,
    ENVIADO,
    APROVADO,
    REJEITADO,
    EXPIRADO,
    CANCELADO
}
