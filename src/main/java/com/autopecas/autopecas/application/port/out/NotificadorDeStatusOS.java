package com.autopecas.autopecas.application.port.out;

/**
 * Port de saída que avisa o cliente quando o status da sua Ordem de Serviço muda.
 *
 * <p>A aplicação declara <b>o que</b> comunicar; o canal é decisão do adapter — hoje e-mail,
 * amanhã SMS ou push, sem que o caso de uso mude.
 *
 * <p>Duas garantias que o contrato exige de qualquer implementação:
 * <ul>
 *   <li><b>Nunca lançar.</b> A notificação é efeito colateral do avanço de status, não parte
 *       dele. Um servidor de e-mail fora do ar não pode impedir a oficina de tocar a OS.</li>
 *   <li><b>Não bloquear indefinidamente.</b> É chamada depois do commit, fora da transação.</li>
 * </ul>
 */
public interface NotificadorDeStatusOS {

    /** Envia o aviso. Implementações registram a falha e seguem — nunca propagam exceção. */
    void notificarMudancaDeStatus(MudancaDeStatus mudanca);

    /**
     * Dados do aviso, já resolvidos pelo caso de uso.
     *
     * @param statusAnterior nulo na abertura da OS
     * @param observacao     texto opcional informado por quem avançou o status
     */
    record MudancaDeStatus(
            String numeroOS,
            String statusAnterior,
            String novoStatus,
            String nomeCliente,
            String emailCliente,
            String observacao
    ) {
    }
}
