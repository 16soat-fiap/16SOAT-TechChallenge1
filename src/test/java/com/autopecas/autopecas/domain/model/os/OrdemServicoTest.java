package com.autopecas.autopecas.domain.model.os;

import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.domain.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes do agregado OrdemServico.
 *
 * <p>São testes puros: nenhum mock, nenhum contexto Spring, nenhum banco. O tempo entra por
 * parâmetro, então as asserções sobre datas são exatas em vez de aproximadas — foi essa a
 * motivação para a port Relogio.
 */
@DisplayName("OrdemServico")
class OrdemServicoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 10, 14, 30);
    private static final UUID CLIENTE_ID = UUID.randomUUID();
    private static final UUID VEICULO_ID = UUID.randomUUID();

    private static OrdemServico osNova() {
        return OrdemServico.abrir("OS-000001", CLIENTE_ID, VEICULO_ID, "Barulho no motor",
                "Cliente ciente do desgaste dos pneus", 50000, null);
    }

    /** OS reconstituída diretamente em um status, para exercitar transições sem encadear todas. */
    private static OrdemServico osNoStatus(StatusOS status) {
        return OrdemServico.reconstituir(UUID.randomUUID(), "OS-000002", 1L, status, 50000, null,
                null, "Queixa", BigDecimal.ZERO, null, null, null, AGORA, AGORA, CLIENTE_ID,
                VEICULO_ID, null, null, null, List.of(), List.of());
    }

    @Nested
    @DisplayName("Abertura")
    class Abertura {

        @Test
        @DisplayName("deve abrir em RECEBIDA, sem id e com valor aprovado zerado")
        void deveAbrirEmRecebida() {
            OrdemServico os = osNova();

            assertThat(os.getStatus()).isEqualTo(StatusOS.RECEBIDA);
            assertThat(os.isNovo()).isTrue();
            assertThat(os.getId()).isNull();
            assertThat(os.getValorTotalAprovado()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(os.getNumero()).isEqualTo("OS-000001");
            assertThat(os.getItensServico()).isEmpty();
            assertThat(os.getItensPeca()).isEmpty();
        }

        @Test
        @DisplayName("deve exigir número, cliente e veículo")
        void deveExigirCamposObrigatorios() {
            assertThatThrownBy(() -> OrdemServico.abrir(null, CLIENTE_ID, VEICULO_ID, "q", null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Número da OS");

            assertThatThrownBy(() -> OrdemServico.abrir("OS-1", null, VEICULO_ID, "q", null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cliente");

            assertThatThrownBy(() -> OrdemServico.abrir("OS-1", CLIENTE_ID, null, "q", null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("veículo");
        }
    }

    @Nested
    @DisplayName("Transições de status")
    class Transicoes {

        static Stream<Arguments> transicoesPermitidas() {
            return Stream.of(
                    Arguments.of(StatusOS.RECEBIDA, StatusOS.EM_DIAGNOSTICO),
                    Arguments.of(StatusOS.EM_DIAGNOSTICO, StatusOS.AGUARDANDO_APROVACAO),
                    Arguments.of(StatusOS.AGUARDANDO_APROVACAO, StatusOS.EM_EXECUCAO),
                    Arguments.of(StatusOS.AGUARDANDO_APROVACAO, StatusOS.EM_DIAGNOSTICO),
                    Arguments.of(StatusOS.EM_EXECUCAO, StatusOS.FINALIZADA),
                    Arguments.of(StatusOS.FINALIZADA, StatusOS.ENTREGUE));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @MethodSource("transicoesPermitidas")
        @DisplayName("deve aceitar as transições previstas no fluxo")
        void deveAceitarTransicoesPermitidas(StatusOS de, StatusOS para) {
            OrdemServico os = osNoStatus(de);

            os.avancarStatus(para, AGORA);

            assertThat(os.getStatus()).isEqualTo(para);
        }

        static Stream<Arguments> transicoesProibidas() {
            return Stream.of(
                    Arguments.of(StatusOS.RECEBIDA, StatusOS.ENTREGUE),
                    Arguments.of(StatusOS.RECEBIDA, StatusOS.EM_EXECUCAO),
                    Arguments.of(StatusOS.EM_DIAGNOSTICO, StatusOS.FINALIZADA),
                    Arguments.of(StatusOS.EM_EXECUCAO, StatusOS.ENTREGUE),
                    Arguments.of(StatusOS.FINALIZADA, StatusOS.EM_EXECUCAO));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @MethodSource("transicoesProibidas")
        @DisplayName("deve recusar transições fora do fluxo, sem alterar o status")
        void deveRecusarTransicoesProibidas(StatusOS de, StatusOS para) {
            OrdemServico os = osNoStatus(de);

            assertThatThrownBy(() -> os.avancarStatus(para, AGORA))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Transição de status inválida");

            assertThat(os.getStatus()).isEqualTo(de);
        }

        @ParameterizedTest
        @EnumSource(StatusOS.class)
        @DisplayName("ENTREGUE é estado final: nenhuma transição sai dele")
        void entregueEhEstadoFinal(StatusOS destino) {
            OrdemServico os = osNoStatus(StatusOS.ENTREGUE);

            assertThatThrownBy(() -> os.avancarStatus(destino, AGORA))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("deve registrar a data do marco de cada transição")
        void deveRegistrarDatasDosMarcos() {
            OrdemServico os = osNoStatus(StatusOS.AGUARDANDO_APROVACAO);

            os.avancarStatus(StatusOS.EM_EXECUCAO, AGORA);
            assertThat(os.getDataInicioExecucao()).isEqualTo(AGORA);
            assertThat(os.getDataFinalizacao()).isNull();

            LocalDateTime fim = AGORA.plusHours(3);
            os.avancarStatus(StatusOS.FINALIZADA, fim);
            assertThat(os.getDataFinalizacao()).isEqualTo(fim);

            LocalDateTime entrega = fim.plusDays(1);
            os.avancarStatus(StatusOS.ENTREGUE, entrega);
            assertThat(os.getDataEntrega()).isEqualTo(entrega);
        }

        @Test
        @DisplayName("transições sem marco não devem gravar datas")
        void transicoesSemMarcoNaoGravamDatas() {
            OrdemServico os = osNoStatus(StatusOS.RECEBIDA);

            os.avancarStatus(StatusOS.EM_DIAGNOSTICO, AGORA);

            assertThat(os.getDataInicioExecucao()).isNull();
            assertThat(os.getDataFinalizacao()).isNull();
            assertThat(os.getDataEntrega()).isNull();
        }
    }

    @Nested
    @DisplayName("Diagnóstico")
    class Diagnostico {

        @Test
        @DisplayName("deve registrar diagnóstico quando a OS está EM_DIAGNOSTICO")
        void deveRegistrarQuandoEmDiagnostico() {
            OrdemServico os = osNoStatus(StatusOS.EM_DIAGNOSTICO);

            os.registrarDiagnostico("Rolamento dianteiro direito com folga");

            assertThat(os.getDiagnostico()).isEqualTo("Rolamento dianteiro direito com folga");
        }

        @ParameterizedTest
        @EnumSource(value = StatusOS.class, names = "EM_DIAGNOSTICO", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("deve recusar diagnóstico em qualquer outro status")
        void deveRecusarForaDeDiagnostico(StatusOS status) {
            OrdemServico os = osNoStatus(status);

            assertThatThrownBy(() -> os.registrarDiagnostico("qualquer"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("EM_DIAGNOSTICO");

            assertThat(os.getDiagnostico()).isNull();
        }
    }

    @Nested
    @DisplayName("Mecânico responsável")
    class MecanicoResponsavel {

        @Test
        @DisplayName("deve atribuir o mecânico informado")
        void deveAtribuirMecanico() {
            OrdemServico os = osNova();
            UUID mecanicoId = UUID.randomUUID();

            os.atribuirMecanico(mecanicoId);

            assertThat(os.getMecanicoResponsavelId()).isEqualTo(mecanicoId);
        }

        @Test
        @DisplayName("deve recusar mecânico nulo")
        void deveRecusarMecanicoNulo() {
            OrdemServico os = osNova();

            assertThatThrownBy(() -> os.atribuirMecanico(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Mecânico responsável");
        }
    }

    @Nested
    @DisplayName("Aprovação de orçamento")
    class AprovacaoDeOrcamento {

        @Test
        @DisplayName("deve copiar o valor aprovado e avançar para EM_EXECUCAO")
        void deveCopiarValorEAvancar() {
            OrdemServico os = osNoStatus(StatusOS.AGUARDANDO_APROVACAO);

            os.registrarAprovacaoDeOrcamento(new BigDecimal("1250.50"), AGORA);

            assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
            assertThat(os.getValorTotalAprovado()).isEqualByComparingTo("1250.50");
            assertThat(os.getDataInicioExecucao()).isEqualTo(AGORA);
        }

        @Test
        @DisplayName("não deve alterar o valor se a OS não puder avançar")
        void naoDeveAlterarValorSeTransicaoInvalida() {
            OrdemServico os = osNoStatus(StatusOS.RECEBIDA);

            assertThatThrownBy(() -> os.registrarAprovacaoDeOrcamento(new BigDecimal("900"), AGORA))
                    .isInstanceOf(BusinessException.class);

            assertThat(os.getValorTotalAprovado()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(os.getStatus()).isEqualTo(StatusOS.RECEBIDA);
        }
    }

    @Nested
    @DisplayName("Itens executados")
    class ItensExecutados {

        @Test
        @DisplayName("deve acumular itens de serviço e de peça")
        void deveAcumularItens() {
            OrdemServico os = osNova();

            os.adicionarItemServico(ItemServicoOS.criar(UUID.randomUUID(), 2, new BigDecimal("150.00")));
            os.adicionarItemPeca(ItemPecaOS.criar(UUID.randomUUID(), 3, new BigDecimal("50.00")));

            assertThat(os.getItensServico()).hasSize(1);
            assertThat(os.getItensPeca()).hasSize(1);
        }

        @Test
        @DisplayName("deve somar o total executado a partir dos subtotais dos itens")
        void deveSomarTotalExecutado() {
            OrdemServico os = osNova();
            os.adicionarItemServico(ItemServicoOS.criar(UUID.randomUUID(), 2, new BigDecimal("150.00")));
            os.adicionarItemPeca(ItemPecaOS.criar(UUID.randomUUID(), 3, new BigDecimal("50.00")));

            assertThat(os.calcularTotalExecutado()).isEqualByComparingTo("450.00");
        }

        @Test
        @DisplayName("as coleções expostas devem ser imutáveis")
        void colecoesExpostasSaoImutaveis() {
            OrdemServico os = osNova();
            ItemServicoOS item = ItemServicoOS.criar(UUID.randomUUID(), 1, BigDecimal.TEN);

            assertThatThrownBy(() -> os.getItensServico().add(item))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Tempo de execução")
    class TempoDeExecucao {

        @Test
        @DisplayName("deve calcular o tempo entre início e finalização")
        void deveCalcularTempo() {
            OrdemServico os = osNoStatus(StatusOS.AGUARDANDO_APROVACAO);
            os.avancarStatus(StatusOS.EM_EXECUCAO, AGORA);
            os.avancarStatus(StatusOS.FINALIZADA, AGORA.plusMinutes(90));

            assertThat(os.calcularTempoExecucaoMinutos()).isEqualTo(90L);
        }

        @Test
        @DisplayName("deve devolver nulo enquanto a OS não foi finalizada")
        void deveDevolverNuloSemFinalizacao() {
            OrdemServico os = osNoStatus(StatusOS.AGUARDANDO_APROVACAO);
            os.avancarStatus(StatusOS.EM_EXECUCAO, AGORA);

            assertThat(os.calcularTempoExecucaoMinutos()).isNull();
        }

        @Test
        @DisplayName("a versão estática aplica a mesma regra sobre duas datas soltas")
        void versaoEstaticaAplicaMesmaRegra() {
            assertThat(OrdemServico.calcularTempoExecucaoMinutos(AGORA, AGORA.plusMinutes(45)))
                    .isEqualTo(45L);
            assertThat(OrdemServico.calcularTempoExecucaoMinutos(null, AGORA)).isNull();
            assertThat(OrdemServico.calcularTempoExecucaoMinutos(AGORA, null)).isNull();
        }
    }
}
