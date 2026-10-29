package com.autopecas.autopecas.adapter.out.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.autopecas.autopecas.application.port.out.Telemetria;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.newrelic.api.agent.Insights;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

/** Os eventos que os dashboards e alertas do New Relic consultam por NRQL. */
@ExtendWith(MockitoExtension.class)
@DisplayName("TelemetriaNewRelic")
class TelemetriaNewRelicTest {

    @Mock private Insights insights;
    @Captor private ArgumentCaptor<Map<String, Object>> atributos;

    private TelemetriaNewRelic telemetria;

    @BeforeEach
    void setUp() {
        telemetria = new TelemetriaNewRelic(insights, "homologacao");
    }

    @Test
    @DisplayName("OS aberta vira OrdemServicoAberta com número e ambiente")
    void osAberta() {
        telemetria.osAberta("OS-000001");

        verify(insights).recordCustomEvent(eq("OrdemServicoAberta"), atributos.capture());
        assertThat(atributos.getValue())
                .containsEntry("numeroOS", "OS-000001")
                .containsEntry("ambiente", "homologacao");
    }

    @Test
    @DisplayName("troca de status leva origem, destino e tempo no status de origem")
    void statusAlterado() {
        telemetria.statusAlterado(
                new Telemetria.TransicaoDeStatus("OS-000001", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO", 3600L));

        verify(insights).recordCustomEvent(eq("OrdemServicoStatusAlterado"), atributos.capture());
        assertThat(atributos.getValue())
                .containsEntry("statusAnterior", "EM_DIAGNOSTICO")
                .containsEntry("statusNovo", "AGUARDANDO_APROVACAO")
                .containsEntry("segundosNoStatusAnterior", 3600L);
    }

    @Test
    @DisplayName("sem tempo medido o atributo é omitido, para não entrar como zero na média")
    void statusAlteradoSemTempo() {
        telemetria.statusAlterado(
                new Telemetria.TransicaoDeStatus("OS-000001", "RECEBIDA", "EM_DIAGNOSTICO", null));

        verify(insights).recordCustomEvent(eq("OrdemServicoStatusAlterado"), atributos.capture());
        assertThat(atributos.getValue()).doesNotContainKey("segundosNoStatusAnterior");
    }

    @Test
    @DisplayName("falha de processamento identifica operação, tipo e erro")
    void falhaNoProcessamento() {
        telemetria.falhaNoProcessamentoDeOS(
                "avancar_status", true, new BusinessException("Transição inválida"));

        verify(insights).recordCustomEvent(eq("FalhaProcessamentoOS"), atributos.capture());
        assertThat(atributos.getValue())
                .containsEntry("operacao", "avancar_status")
                .containsEntry("regraDeNegocio", true)
                .containsEntry("erro", "BusinessException")
                .containsEntry("mensagem", "Transição inválida");
    }

    @Test
    @DisplayName("mensagem de erro longa é truncada")
    void mensagemLongaTruncada() {
        telemetria.falhaDeIntegracao(
                "email", "notificar_mudanca_de_status", new IllegalStateException("x".repeat(1000)));

        verify(insights).recordCustomEvent(eq("FalhaIntegracao"), atributos.capture());
        assertThat((String) atributos.getValue().get("mensagem")).hasSize(255);
        assertThat(atributos.getValue()).containsEntry("integracao", "email");
    }

    @Test
    @DisplayName("erro sem mensagem não vira atributo nulo")
    void erroSemMensagem() {
        telemetria.falhaDeIntegracao("email", "notificar", new IllegalStateException());

        verify(insights).recordCustomEvent(eq("FalhaIntegracao"), atributos.capture());
        assertThat(atributos.getValue()).containsEntry("mensagem", "");
    }

    @Test
    @DisplayName("falha do agente nunca chega a quem chamou")
    void falhaDoAgenteNaoPropaga() {
        doThrow(new IllegalStateException("agente fora do ar"))
                .when(insights)
                .recordCustomEvent(any(), anyMap());

        assertThatCode(() -> telemetria.osAberta("OS-000001")).doesNotThrowAnyException();
    }
}
