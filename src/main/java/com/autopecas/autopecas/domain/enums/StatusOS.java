package com.autopecas.autopecas.domain.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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
 *
 * <p>Além do ciclo de vida, o enum carrega a regra da <b>fila de trabalho da oficina</b>: quais
 * status ainda demandam ação ({@link #emAndamento()}) e em que ordem de urgência aparecem
 * ({@link #prioridadeNaFila()}). A regra vive aqui, e não na consulta SQL, porque é decisão de
 * negócio — a query apenas a materializa.
 */
public enum StatusOS {

    RECEBIDA(4),
    EM_DIAGNOSTICO(3),
    AGUARDANDO_APROVACAO(2),
    EM_EXECUCAO(1),
    /** Fora da fila operacional: prioridade 5 mantém as encerradas sempre por último. */
    FINALIZADA(5),
    ENTREGUE(5);

    private final int prioridadeNaFila;

    StatusOS(int prioridadeNaFila) {
        this.prioridadeNaFila = prioridadeNaFila;
    }

    /**
     * Ordem de urgência na fila de trabalho: EM_EXECUCAO (1) &gt; AGUARDANDO_APROVACAO (2) &gt;
     * EM_DIAGNOSTICO (3) &gt; RECEBIDA (4). Menor número aparece primeiro.
     */
    public int prioridadeNaFila() {
        return prioridadeNaFila;
    }

    /**
     * OS finalizada ou entregue: o trabalho da oficina acabou e ela sai da fila operacional.
     * A exclusão é lógica — o registro continua no banco e segue acessível por busca direta
     * ou por filtro explícito de status.
     */
    public boolean encerrouAtendimento() {
        return this == FINALIZADA || this == ENTREGUE;
    }

    /** Status que ainda demandam ação da oficina, na ordem em que devem aparecer na fila. */
    public static List<StatusOS> emAndamento() {
        return Arrays.stream(values())
                .filter(status -> !status.encerrouAtendimento())
                .sorted(Comparator.comparingInt(StatusOS::prioridadeNaFila))
                .toList();
    }
}
