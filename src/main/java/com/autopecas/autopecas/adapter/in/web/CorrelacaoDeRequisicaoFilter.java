package com.autopecas.autopecas.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Dá a cada requisição um identificador que aparece em todas as linhas de log que ela produz.
 *
 * <p>Quem chama pode mandar o seu em {@value #CABECALHO} — assim um cliente ou outro serviço
 * encontra as linhas da própria chamada. Sem cabeçalho, ou com um valor fora do formato, o
 * filtro gera um UUID. O identificador volta na resposta, no mesmo cabeçalho.
 *
 * <p>Vai para o MDC, que o log estruturado (JSON) inclui em cada linha. No New Relic, o agente
 * ainda liga cada linha ao trace da requisição; o {@code correlationId} é o que sobrevive fora
 * da ferramenta, no {@code kubectl logs}.
 *
 * <p>Ordem máxima: roda antes do Spring Security, para que até os logs de autenticação recusada
 * saiam correlacionados.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelacaoDeRequisicaoFilter extends OncePerRequestFilter {

    static final String CABECALHO = "X-Correlation-Id";
    static final String CHAVE_MDC = "correlationId";

    /** Recusa qualquer coisa que pudesse injetar texto no log: só letras, dígitos e hífen. */
    private static final Pattern FORMATO_ACEITO = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String correlationId = idRecebidoOuNovo(request.getHeader(CABECALHO));
        MDC.put(CHAVE_MDC, correlationId);
        response.setHeader(CABECALHO, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            // A thread volta para o pool do Tomcat: sem limpar, a próxima requisição herdaria o id.
            MDC.remove(CHAVE_MDC);
        }
    }

    private static String idRecebidoOuNovo(String recebido) {
        if (recebido != null && FORMATO_ACEITO.matcher(recebido).matches()) {
            return recebido;
        }
        return UUID.randomUUID().toString();
    }
}
