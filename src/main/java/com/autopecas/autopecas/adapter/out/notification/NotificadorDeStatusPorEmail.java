package com.autopecas.autopecas.adapter.out.notification;

import com.autopecas.autopecas.application.port.out.NotificadorDeStatusOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Adapter que avisa o cliente por e-mail quando o status da sua OS muda.
 *
 * <p>O {@link JavaMailSender} entra por {@link ObjectProvider} porque o Spring Boot só o
 * autoconfigura quando {@code spring.mail.host} está definido. Sem servidor configurado — caso
 * do Compose e dos testes — o adapter registra a notificação em log em vez de falhar na
 * subida do contexto. É o mesmo comportamento de um canal indisponível, que o contrato da port
 * já exige tratar.
 *
 * <p>Nenhum caminho propaga exceção: o javadoc de {@link NotificadorDeStatusOS} explica por quê.
 */
@Component
public class NotificadorDeStatusPorEmail implements NotificadorDeStatusOS {

    private static final Logger log = LoggerFactory.getLogger(NotificadorDeStatusPorEmail.class);

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${notificacoes.email.remetente:nao-responda@autopecas.local}")
    private String remetente;

    public NotificadorDeStatusPorEmail(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void notificarMudancaDeStatus(MudancaDeStatus mudanca) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("E-mail não configurado (spring.mail.host ausente). Notificação da OS {} "
                    + "para {}: {} → {}", mudanca.numeroOS(), mudanca.emailCliente(),
                    mudanca.statusAnterior(), mudanca.novoStatus());
            return;
        }

        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(remetente);
            mensagem.setTo(mudanca.emailCliente());
            mensagem.setSubject("OS " + mudanca.numeroOS() + " — " + porExtenso(mudanca.novoStatus()));
            mensagem.setText(corpo(mudanca));
            sender.send(mensagem);
            log.info("Notificação de status enviada para a OS {}", mudanca.numeroOS());
        } catch (RuntimeException e) {
            // A OS já avançou; o e-mail é consequência, não condição.
            log.warn("Falha ao notificar o cliente da OS {} — o status permanece atualizado",
                    mudanca.numeroOS(), e);
        }
    }

    private String corpo(MudancaDeStatus mudanca) {
        StringBuilder texto = new StringBuilder()
                .append("Olá, ").append(mudanca.nomeCliente()).append(".\n\n")
                .append("A sua ordem de serviço ").append(mudanca.numeroOS())
                .append(" mudou de situação.\n\n");

        if (mudanca.statusAnterior() != null) {
            texto.append("De: ").append(porExtenso(mudanca.statusAnterior())).append('\n');
        }
        texto.append("Para: ").append(porExtenso(mudanca.novoStatus())).append('\n');

        if (mudanca.observacao() != null && !mudanca.observacao().isBlank()) {
            texto.append("\nObservação da oficina: ").append(mudanca.observacao()).append('\n');
        }

        return texto.append("\nEsta é uma mensagem automática — não responda a este e-mail.\n")
                .toString();
    }

    /** Converte o nome do enum no rótulo que o cliente lê: EM_DIAGNOSTICO → "Em diagnostico". */
    private String porExtenso(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        String comEspacos = status.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(comEspacos.charAt(0)) + comEspacos.substring(1);
    }
}
