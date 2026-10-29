package com.autopecas.autopecas.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/** O identificador que liga todas as linhas de log de uma requisição. */
@DisplayName("CorrelacaoDeRequisicaoFilter")
class CorrelacaoDeRequisicaoFilterTest {

    private final CorrelacaoDeRequisicaoFilter filtro = new CorrelacaoDeRequisicaoFilter();

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    /** Executa o filtro e devolve o id que estava no MDC enquanto a requisição era atendida. */
    private String idDuranteARequisicao(MockHttpServletRequest request,
                                        MockHttpServletResponse response) throws Exception {
        AtomicReference<String> noMdc = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req,
                                 jakarta.servlet.ServletResponse res) {
                noMdc.set(MDC.get(CorrelacaoDeRequisicaoFilter.CHAVE_MDC));
            }
        };
        filtro.doFilter(request, response, chain);
        return noMdc.get();
    }

    @Test
    @DisplayName("reaproveita o id recebido e o devolve na resposta")
    void reaproveitaIdRecebido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelacaoDeRequisicaoFilter.CABECALHO, "pedido-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String id = idDuranteARequisicao(request, response);

        assertThat(id).isEqualTo("pedido-123");
        assertThat(response.getHeader(CorrelacaoDeRequisicaoFilter.CABECALHO)).isEqualTo("pedido-123");
    }

    @Test
    @DisplayName("sem cabeçalho gera um UUID")
    void geraIdQuandoAusente() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String id = idDuranteARequisicao(new MockHttpServletRequest(), response);

        assertThat(UUID.fromString(id)).isNotNull();
        assertThat(response.getHeader(CorrelacaoDeRequisicaoFilter.CABECALHO)).isEqualTo(id);
    }

    @Test
    @DisplayName("recusa valor que poderia injetar texto no log")
    void recusaValorForaDoFormato() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelacaoDeRequisicaoFilter.CABECALHO, "abc\n{\"nivel\":\"ERROR\"}");

        String id = idDuranteARequisicao(request, new MockHttpServletResponse());

        assertThat(UUID.fromString(id)).isNotNull();
    }

    @Test
    @DisplayName("limpa o MDC ao final, para a próxima requisição da thread não herdar o id")
    void limpaMdcAoFinal() throws Exception {
        idDuranteARequisicao(new MockHttpServletRequest(), new MockHttpServletResponse());

        assertThat(MDC.get(CorrelacaoDeRequisicaoFilter.CHAVE_MDC)).isNull();
    }
}
