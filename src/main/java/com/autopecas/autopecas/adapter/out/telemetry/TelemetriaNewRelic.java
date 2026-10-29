package com.autopecas.autopecas.adapter.out.telemetry;

import com.autopecas.autopecas.application.port.out.Telemetria;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.NewRelic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter que publica os fatos de negócio como eventos customizados do New Relic.
 *
 * <p>Cada método vira um tipo de evento consultável por NRQL — é deles que saem os dashboards
 * (volume diário, tempo médio por status, erros de integração) e o alerta de falha no
 * processamento de OS. O mesmo fato também vai para o log estruturado, com os mesmos campos: o
 * agente encaminha o log já ligado ao trace da requisição.
 *
 * <p>Sem o agente na JVM — testes, Compose, dev —, a API do New Relic é no-op: o adapter fica
 * inerte sem que a aplicação precise de configuração condicional.
 *
 * <p>Nenhum caminho propaga exceção: o javadoc de {@link Telemetria} explica por quê.
 */
@Component
public class TelemetriaNewRelic implements Telemetria {

    static final String EVENTO_OS_ABERTA = "OrdemServicoAberta";
    static final String EVENTO_STATUS_ALTERADO = "OrdemServicoStatusAlterado";
    static final String EVENTO_FALHA_OS = "FalhaProcessamentoOS";
    static final String EVENTO_FALHA_INTEGRACAO = "FalhaIntegracao";

    /** Bem abaixo do limite de atributo do New Relic; mensagem de exceção não precisa de mais. */
    private static final int TAMANHO_MAXIMO_MENSAGEM = 255;

    private static final Logger log = LoggerFactory.getLogger(TelemetriaNewRelic.class);

    private final Insights insights;
    private final String ambiente;

    @Autowired
    public TelemetriaNewRelic(@Value("${observabilidade.ambiente:local}") String ambiente) {
        this(NewRelic.getAgent().getInsights(), ambiente);
    }

    TelemetriaNewRelic(Insights insights, String ambiente) {
        this.insights = insights;
        this.ambiente = ambiente;
    }

    @Override
    public void osAberta(String numeroOS) {
        Map<String, Object> atributos = atributosBase();
        atributos.put("numeroOS", numeroOS);
        publicar(EVENTO_OS_ABERTA, atributos);
        log.atInfo().addKeyValue("numeroOS", numeroOS).log("Ordem de serviço aberta");
    }

    @Override
    public void statusAlterado(TransicaoDeStatus transicao) {
        Map<String, Object> atributos = atributosBase();
        atributos.put("numeroOS", transicao.numeroOS());
        atributos.put("statusAnterior", transicao.statusAnterior());
        atributos.put("statusNovo", transicao.statusNovo());
        if (transicao.segundosNoAnterior() != null) {
            atributos.put("segundosNoStatusAnterior", transicao.segundosNoAnterior());
        }
        publicar(EVENTO_STATUS_ALTERADO, atributos);
        log.atInfo()
                .addKeyValue("numeroOS", transicao.numeroOS())
                .addKeyValue("statusAnterior", transicao.statusAnterior())
                .addKeyValue("statusNovo", transicao.statusNovo())
                .addKeyValue("segundosNoStatusAnterior", transicao.segundosNoAnterior())
                .log("Status da ordem de serviço alterado");
    }

    @Override
    public void falhaNoProcessamentoDeOS(
            String operacao, boolean regraDeNegocio, RuntimeException erro) {
        Map<String, Object> atributos = atributosDoErro(erro);
        atributos.put("operacao", operacao);
        atributos.put("regraDeNegocio", regraDeNegocio);
        publicar(EVENTO_FALHA_OS, atributos);
        if (regraDeNegocio) {
            log.atInfo()
                    .addKeyValue("operacao", operacao)
                    .addKeyValue("motivo", atributos.get("mensagem"))
                    .log("Operação de ordem de serviço recusada por regra de negócio");
        } else {
            log.atError()
                    .addKeyValue("operacao", operacao)
                    .setCause(erro)
                    .log("Falha inesperada no processamento de ordem de serviço");
        }
    }

    @Override
    public void falhaDeIntegracao(String integracao, String operacao, RuntimeException erro) {
        Map<String, Object> atributos = atributosDoErro(erro);
        atributos.put("integracao", integracao);
        atributos.put("operacao", operacao);
        publicar(EVENTO_FALHA_INTEGRACAO, atributos);
    }

    private Map<String, Object> atributosBase() {
        Map<String, Object> atributos = new HashMap<>();
        atributos.put("ambiente", ambiente);
        return atributos;
    }

    private Map<String, Object> atributosDoErro(RuntimeException erro) {
        Map<String, Object> atributos = atributosBase();
        atributos.put("erro", erro.getClass().getSimpleName());
        atributos.put("mensagem", truncar(erro.getMessage()));
        return atributos;
    }

    private void publicar(String tipo, Map<String, Object> atributos) {
        try {
            insights.recordCustomEvent(tipo, atributos);
        } catch (RuntimeException e) {
            log.debug("Evento {} não publicado no New Relic", tipo, e);
        }
    }

    private static String truncar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= TAMANHO_MAXIMO_MENSAGEM
                ? texto
                : texto.substring(0, TAMANHO_MAXIMO_MENSAGEM);
    }
}
