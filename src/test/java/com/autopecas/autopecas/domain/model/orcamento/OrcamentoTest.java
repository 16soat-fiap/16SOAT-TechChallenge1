package com.autopecas.autopecas.domain.model.orcamento;

import com.autopecas.autopecas.domain.enums.StatusOrcamento;
import com.autopecas.autopecas.domain.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testes do agregado Orcamento: ciclo de vida, recálculo de valores e validade. */
@DisplayName("Orcamento")
class OrcamentoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 10, 14, 30);
    private static final LocalDate HOJE = AGORA.toLocalDate();
    private static final UUID OS_ID = UUID.randomUUID();

    private static Orcamento rascunho() {
        return Orcamento.criar(OS_ID, 1, "50% na entrada", 5, HOJE.plusDays(7), "sem observações", null);
    }

    private static Orcamento noStatus(StatusOrcamento status) {
        return noStatusComValidade(status, HOJE.plusDays(7));
    }

    private static Orcamento noStatusComValidade(StatusOrcamento status, LocalDate validade) {
        return Orcamento.reconstituir(UUID.randomUUID(), 1, status, 0L, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, validade, null,
                null, null, null, AGORA, AGORA, OS_ID, null, List.of(), List.of());
    }

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("deve criar em RASCUNHO com valores zerados")
        void deveCriarEmRascunho() {
            Orcamento orcamento = rascunho();

            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.RASCUNHO);
            assertThat(orcamento.getVersao()).isEqualTo(1);
            assertThat(orcamento.isNovo()).isTrue();
            assertThat(orcamento.getValorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(orcamento.isAprovado()).isFalse();
        }

        @Test
        @DisplayName("deve exigir OS e versão positiva")
        void deveExigirOsEVersao() {
            assertThatThrownBy(() -> Orcamento.criar(null, 1, null, null, null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ordem de serviço");

            assertThatThrownBy(() -> Orcamento.criar(OS_ID, 0, null, null, null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Versão");
        }
    }

    @Nested
    @DisplayName("Recálculo de valores")
    class Recalculo {

        @Test
        @DisplayName("deve somar mão de obra, peças e acréscimo")
        void deveSomarComponentes() {
            Orcamento orcamento = rascunho();
            orcamento.adicionarItemServico(
                    ItemOrcamentoServico.criar(UUID.randomUUID(), 2, new BigDecimal("150.00")));
            orcamento.adicionarItemPeca(
                    ItemOrcamentoPeca.criar(UUID.randomUUID(), 3, new BigDecimal("50.00")));

            orcamento.recalcular();

            assertThat(orcamento.getValorMaoObra()).isEqualByComparingTo("300.00");
            assertThat(orcamento.getValorPecas()).isEqualByComparingTo("150.00");
            assertThat(orcamento.getValorTotal()).isEqualByComparingTo("450.00");
        }

        @Test
        @DisplayName("deve zerar os valores quando não há itens")
        void deveZerarSemItens() {
            Orcamento orcamento = rascunho();

            orcamento.recalcular();

            assertThat(orcamento.getValorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("recalcular duas vezes não deve acumular valores")
        void recalcularEhIdempotente() {
            Orcamento orcamento = rascunho();
            orcamento.adicionarItemPeca(
                    ItemOrcamentoPeca.criar(UUID.randomUUID(), 1, new BigDecimal("100.00")));

            orcamento.recalcular();
            orcamento.recalcular();

            assertThat(orcamento.getValorTotal()).isEqualByComparingTo("100.00");
        }
    }

    @Nested
    @DisplayName("Envio")
    class Envio {

        @Test
        @DisplayName("deve enviar um rascunho e registrar a data")
        void deveEnviarRascunho() {
            Orcamento orcamento = rascunho();

            orcamento.enviar(AGORA);

            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.ENVIADA);
            assertThat(orcamento.getDataEnvio()).isEqualTo(AGORA);
        }

        @ParameterizedTest
        @EnumSource(value = StatusOrcamento.class, names = "RASCUNHO", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("deve recusar envio fora de RASCUNHO")
        void deveRecusarForaDeRascunho(StatusOrcamento status) {
            Orcamento orcamento = noStatus(status);

            assertThatThrownBy(() -> orcamento.enviar(AGORA))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Rascunho");
        }
    }

    @Nested
    @DisplayName("Resposta do cliente")
    class RespostaDoCliente {

        @Test
        @DisplayName("deve aprovar um orçamento enviado")
        void deveAprovarEnviado() {
            Orcamento orcamento = noStatus(StatusOrcamento.ENVIADA);

            orcamento.aprovar(AGORA);

            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.APROVADA);
            assertThat(orcamento.isAprovado()).isTrue();
            assertThat(orcamento.getDataRespostaCliente()).isEqualTo(AGORA);
        }

        @Test
        @DisplayName("deve rejeitar um orçamento enviado guardando o motivo")
        void deveRejeitarEnviado() {
            Orcamento orcamento = noStatus(StatusOrcamento.ENVIADA);

            orcamento.rejeitar("Valor acima do esperado", AGORA);

            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.CANCELADA);
            assertThat(orcamento.getMotivoRejeicao()).isEqualTo("Valor acima do esperado");
            assertThat(orcamento.getDataRespostaCliente()).isEqualTo(AGORA);
        }

        @Test
        @DisplayName("deve aceitar rejeição sem motivo")
        void deveAceitarRejeicaoSemMotivo() {
            Orcamento orcamento = noStatus(StatusOrcamento.ENVIADA);

            orcamento.rejeitar(null, AGORA);

            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.CANCELADA);
            assertThat(orcamento.getMotivoRejeicao()).isNull();
        }

        @ParameterizedTest
        @EnumSource(value = StatusOrcamento.class, names = "ENVIADA", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("deve recusar aprovação e rejeição fora de ENVIADA")
        void deveRecusarForaDeEnviada(StatusOrcamento status) {
            Orcamento paraAprovar = noStatus(status);
            Orcamento paraRejeitar = noStatus(status);

            assertThatThrownBy(() -> paraAprovar.aprovar(AGORA))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> paraRejeitar.rejeitar("motivo", AGORA))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Validade")
    class Validade {

        @Test
        @DisplayName("deve considerar expirado quando a validade já passou")
        void deveConsiderarExpirado() {
            Orcamento orcamento = noStatusComValidade(StatusOrcamento.ENVIADA, HOJE.minusDays(1));

            assertThat(orcamento.estaExpirado(HOJE)).isTrue();
            assertThat(orcamento.estaValido(HOJE)).isFalse();
        }

        @Test
        @DisplayName("a validade no próprio dia ainda vale")
        void validadeNoDiaAindaVale() {
            Orcamento orcamento = noStatusComValidade(StatusOrcamento.ENVIADA, HOJE);

            assertThat(orcamento.estaExpirado(HOJE)).isFalse();
            assertThat(orcamento.estaValido(HOJE)).isTrue();
        }

        @Test
        @DisplayName("sem data de validade nunca expira")
        void semValidadeNuncaExpira() {
            Orcamento orcamento = noStatusComValidade(StatusOrcamento.ENVIADA, null);

            assertThat(orcamento.estaExpirado(HOJE)).isFalse();
        }

        @Test
        @DisplayName("expirar deve marcar apenas orçamentos enviados e vencidos")
        void expirarSoAfetaEnviadoVencido() {
            Orcamento vencido = noStatusComValidade(StatusOrcamento.ENVIADA, HOJE.minusDays(1));
            vencido.expirar(HOJE);
            assertThat(vencido.getStatus()).isEqualTo(StatusOrcamento.EXPIRADO);

            Orcamento noPrazo = noStatusComValidade(StatusOrcamento.ENVIADA, HOJE.plusDays(1));
            noPrazo.expirar(HOJE);
            assertThat(noPrazo.getStatus()).isEqualTo(StatusOrcamento.ENVIADA);

            Orcamento aprovadoVencido = noStatusComValidade(StatusOrcamento.APROVADA, HOJE.minusDays(1));
            aprovadoVencido.expirar(HOJE);
            assertThat(aprovadoVencido.getStatus()).isEqualTo(StatusOrcamento.APROVADA);
        }
    }
}
