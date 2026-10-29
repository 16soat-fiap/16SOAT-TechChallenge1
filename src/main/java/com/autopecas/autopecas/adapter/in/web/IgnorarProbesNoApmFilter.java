package com.autopecas.autopecas.adapter.in.web;

import com.newrelic.api.agent.NewRelic;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Tira do APM as chamadas do Actuator — as probes do Kubernetes.
 *
 * <p>Cada pod é sondado a cada 5–20 segundos por três probes. Contadas como transações, elas
 * seriam a maior parte do throughput e, por serem as respostas mais rápidas da aplicação,
 * puxariam a latência média para baixo: o painel mostraria um serviço mais saudável do que o
 * cliente percebe. A disponibilidade continua medida — pelo monitor sintético e pelo estado dos
 * pods na integração com o Kubernetes.
 *
 * <p>Sem o agente na JVM, {@code ignoreTransaction} é no-op.
 */
@Component
public class IgnorarProbesNoApmFilter extends OncePerRequestFilter {

    private static final String PREFIXO_ACTUATOR = "/actuator";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIXO_ACTUATOR);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        NewRelic.ignoreTransaction();
        chain.doFilter(request, response);
    }
}
