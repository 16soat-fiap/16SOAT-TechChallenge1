package com.autopecas.autopecas.application.port.out;

/**
 * Port de saída para os fatos de negócio que a operação precisa enxergar em tempo real.
 *
 * <p>A aplicação declara <b>o que aconteceu</b> — uma OS aberta, uma troca de status, uma falha;
 * a ferramenta de observabilidade que recebe é decisão do adapter. São esses fatos que alimentam
 * os dashboards de volume diário e de tempo por status e os alertas de falha no processamento.
 *
 * <p>Mesma garantia do {@link NotificadorDeStatusOS}: <b>nunca lançar</b>. Telemetria indisponível
 * não pode impedir a oficina de abrir ou avançar uma OS.
 */
public interface Telemetria {

    /** Uma OS foi aberta e confirmada no banco. */
    void osAberta(String numeroOS);

    /** Uma OS mudou de status, com o tempo que permaneceu no status de origem. */
    void statusAlterado(TransicaoDeStatus transicao);

    /**
     * Uma operação sobre a OS falhou.
     *
     * @param operacao     o caso de uso: "abrir", "avancar_status", ...
     * @param regraDeNegocio true quando a falha é uma regra recusando o pedido (transição
     *                       inválida, OS inexistente); false quando é erro inesperado — o que
     *                       deve disparar alerta
     */
    void falhaNoProcessamentoDeOS(String operacao, boolean regraDeNegocio, RuntimeException erro);

    /** Uma integração externa (e-mail, por exemplo) falhou sem afetar a operação principal. */
    void falhaDeIntegracao(String integracao, String operacao, RuntimeException erro);

    /**
     * @param statusAnterior      nulo na abertura
     * @param segundosNoAnterior  nulo quando não há registro anterior no histórico
     */
    record TransicaoDeStatus(
            String numeroOS,
            String statusAnterior,
            String statusNovo,
            Long segundosNoAnterior
    ) {
    }
}
