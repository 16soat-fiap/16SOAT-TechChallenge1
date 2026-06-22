package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.domain.enums.StatusOrcamento;
import com.autopecas.autopecas.util.test.ClienteBuilder;
import com.autopecas.autopecas.util.test.FuncionarioBuilder;
import com.autopecas.autopecas.util.test.ItemPecaOSBuilder;
import com.autopecas.autopecas.util.test.ItemServicoOSBuilder;
import com.autopecas.autopecas.util.test.OrcamentoBuilder;
import com.autopecas.autopecas.util.test.OrdemServicoBuilder;
import com.autopecas.autopecas.util.test.PecaBuilder;
import com.autopecas.autopecas.util.test.ServicoBuilder;
import com.autopecas.autopecas.util.test.VeiculoBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrdemServico")
class OrdemServicoTest {

    private Cliente cliente;
    private Veiculo veiculo;
    private Atendente atendenteRecepcao;
    private Mecanico mecanicoResponsavel;
    private Atendente atendenteEntrega;

    @BeforeEach
    void setUp() {
        cliente = ClienteBuilder.clientePF().build();
        veiculo = VeiculoBuilder.veiculo(cliente).build();
        atendenteRecepcao = FuncionarioBuilder.atendente().build();
        mecanicoResponsavel = FuncionarioBuilder.mecanico().build();
        atendenteEntrega = FuncionarioBuilder.atendente().build();
    }

    @Nested
    @DisplayName("Testes de Construtor e Builder")
    class ConstrutorBuilderTests {

        @Test
        @DisplayName("deve criar OrdemServico com builder e valores padrão do builder")
        void deveCriarOrdemServicoComBuilderEValoresPadrao() {
            // Given
            OrdemServico os = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendenteRecepcao).build();

            // Then
            assertThat(os).isNotNull();
            assertThat(os.getId()).isNotNull();
            assertThat(os.getNumero()).isNull(); // Gerado pela service
            assertThat(os.getVersion()).isNull(); // Gerado pela persistência
            assertThat(os.getStatus()).isEqualTo(StatusOS.RECEBIDA);
            assertThat(os.getQuilometragemEntrada()).isEqualTo(50000);
            assertThat(os.getObservacoesEntrada()).isNull();
            assertThat(os.getDiagnostico()).isNull();
            assertThat(os.getQueixaCliente()).isEqualTo("Barulho estranho no motor");
            assertThat(os.getValorTotalAprovado()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(os.getDataInicioExecucao()).isNull();
            assertThat(os.getDataFinalizacao()).isNull();
            assertThat(os.getDataEntrega()).isNull();
            assertThat(os.getCreatedAt()).isNull(); // Gerado pela persistência
            assertThat(os.getUpdatedAt()).isNull(); // Gerado pela persistência
            assertThat(os.getCliente()).isEqualTo(cliente);
            assertThat(os.getVeiculo()).isEqualTo(veiculo);
            assertThat(os.getAtendenteRecepcao()).isEqualTo(atendenteRecepcao);
            assertThat(os.getMecanicoResponsavel()).isNull();
            assertThat(os.getAtendenteEntrega()).isNull();
            assertThat(os.getItensServico()).isEmpty();
            assertThat(os.getItensPeca()).isEmpty();
            assertThat(os.getOrcamentos()).isEmpty();
            assertThat(os.getHistorico()).isEmpty();
        }

        @Test
        @DisplayName("deve criar OrdemServico com builder e todos os valores fornecidos")
        void deveCriarOrdemServicoComBuilderEValoresFornecidos() {
            // Given
            UUID id = UUID.randomUUID();
            String numero = "OS-001";
            Long version = 1L;
            StatusOS status = StatusOS.ENTREGUE;
            Integer quilometragemEntrada = 60000;
            String observacoesEntrada = "Observações iniciais";
            String diagnostico = "Diagnóstico técnico";
            String queixaCliente = "Queixa do cliente";
            BigDecimal valorTotalAprovado = new BigDecimal("500.00");
            LocalDateTime dataInicioExecucao = LocalDateTime.now().minusDays(2);
            LocalDateTime dataFinalizacao = LocalDateTime.now().minusDays(1);
            LocalDateTime dataEntrega = LocalDateTime.now();

            // When
            OrdemServico os = OrdemServico.builder()
                    .id(id)
                    .numero(numero)
                    .version(version)
                    .status(status)
                    .quilometragemEntrada(quilometragemEntrada)
                    .observacoesEntrada(observacoesEntrada)
                    .diagnostico(diagnostico)
                    .queixaCliente(queixaCliente)
                    .valorTotalAprovado(valorTotalAprovado)
                    .dataInicioExecucao(dataInicioExecucao)
                    .dataFinalizacao(dataFinalizacao)
                    .dataEntrega(dataEntrega)
                    .cliente(cliente)
                    .veiculo(veiculo)
                    .atendenteRecepcao(atendenteRecepcao)
                    .mecanicoResponsavel(mecanicoResponsavel)
                    .atendenteEntrega(atendenteEntrega)
                    .build();

            // Then
            assertThat(os).isNotNull();
            assertThat(os.getId()).isEqualTo(id);
            assertThat(os.getNumero()).isEqualTo(numero);
            assertThat(os.getVersion()).isEqualTo(version);
            assertThat(os.getStatus()).isEqualTo(status);
            assertThat(os.getQuilometragemEntrada()).isEqualTo(quilometragemEntrada);
            assertThat(os.getObservacoesEntrada()).isEqualTo(observacoesEntrada);
            assertThat(os.getDiagnostico()).isEqualTo(diagnostico);
            assertThat(os.getQueixaCliente()).isEqualTo(queixaCliente);
            assertThat(os.getValorTotalAprovado()).isEqualByComparingTo(valorTotalAprovado);
            assertThat(os.getDataInicioExecucao()).isEqualTo(dataInicioExecucao);
            assertThat(os.getDataFinalizacao()).isEqualTo(dataFinalizacao);
            assertThat(os.getDataEntrega()).isEqualTo(dataEntrega);
            assertThat(os.getCliente()).isEqualTo(cliente);
            assertThat(os.getVeiculo()).isEqualTo(veiculo);
            assertThat(os.getAtendenteRecepcao()).isEqualTo(atendenteRecepcao);
            assertThat(os.getMecanicoResponsavel()).isEqualTo(mecanicoResponsavel);
            assertThat(os.getAtendenteEntrega()).isEqualTo(atendenteEntrega);
        }
    }

    @Nested
    @DisplayName("Testes de Transição de Status (avancarStatus)")
    class AvancarStatusTests {

        private OrdemServico os;

        @BeforeEach
        void setupAvancarStatus() {
            os = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendenteRecepcao).build();
        }

        @ParameterizedTest(name = "deve avançar de {0} para {1} com sucesso")
        @MethodSource("provideValidStatusTransitions")
        void deveAvancarStatusComSucesso(StatusOS statusInicial, StatusOS statusFinal, boolean shouldSetDataInicioExecucao, boolean shouldSetDataFinalizacao, boolean shouldSetDataEntrega) {
            // Given
            os.setStatus(statusInicial);

            // When
            os.avancarStatus(statusFinal);

            // Then
            assertThat(os.getStatus()).isEqualTo(statusFinal);
            if (shouldSetDataInicioExecucao) {
                assertThat(os.getDataInicioExecucao()).isNotNull();
            }
            if (shouldSetDataFinalizacao) {
                assertThat(os.getDataFinalizacao()).isNotNull();
            }
            if (shouldSetDataEntrega) {
                assertThat(os.getDataEntrega()).isNotNull();
            }
        }

        private static Stream<Arguments> provideValidStatusTransitions() {
            return Stream.of(
                    Arguments.of(StatusOS.RECEBIDA, StatusOS.EM_DIAGNOSTICO, false, false, false),
                    Arguments.of(StatusOS.EM_DIAGNOSTICO, StatusOS.AGUARDANDO_APROVACAO, false, false, false),
                    Arguments.of(StatusOS.AGUARDANDO_APROVACAO, StatusOS.EM_EXECUCAO, true, false, false),
                    Arguments.of(StatusOS.AGUARDANDO_APROVACAO, StatusOS.EM_DIAGNOSTICO, false, false, false), // Retorno para diagnóstico
                    Arguments.of(StatusOS.EM_EXECUCAO, StatusOS.FINALIZADA, false, true, false),
                    Arguments.of(StatusOS.FINALIZADA, StatusOS.ENTREGUE, false, false, true)
            );
        }

        @ParameterizedTest(name = "nao deve avançar de {0} para {1} (transição inválida)")
        @MethodSource("provideInvalidStatusTransitions")
        void naoDeveAvancarStatusComTransicaoInvalida(StatusOS statusInicial, StatusOS statusFinal) {
            // Given
            os.setStatus(statusInicial);

            // When / Then
            assertThatThrownBy(() -> os.avancarStatus(statusFinal))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Transição de status inválida:");
            assertThat(os.getStatus()).isEqualTo(statusInicial); // Status não deve mudar
        }

        private static Stream<Arguments> provideInvalidStatusTransitions() {
            return Stream.of(
                    Arguments.of(StatusOS.RECEBIDA, StatusOS.AGUARDANDO_APROVACAO),
                    Arguments.of(StatusOS.RECEBIDA, StatusOS.EM_EXECUCAO),
                    Arguments.of(StatusOS.EM_DIAGNOSTICO, StatusOS.RECEBIDA),
                    Arguments.of(StatusOS.AGUARDANDO_APROVACAO, StatusOS.RECEBIDA),
                    Arguments.of(StatusOS.EM_EXECUCAO, StatusOS.RECEBIDA),
                    Arguments.of(StatusOS.FINALIZADA, StatusOS.EM_EXECUCAO),
                    Arguments.of(StatusOS.ENTREGUE, StatusOS.FINALIZADA),
                    Arguments.of(StatusOS.ENTREGUE, StatusOS.EM_DIAGNOSTICO)
            );
        }
    }

    @Nested
    @DisplayName("Testes de Gerenciamento de Itens e Orçamentos")
    class GerenciamentoItensOrcamentosTests {

        private OrdemServico os;
        private Servico servico;
        private Peca peca;

        @BeforeEach
        void setupGerenciamento() {
            os = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendenteRecepcao).build();
            servico = ServicoBuilder.servico().build();
            peca = PecaBuilder.peca().build();
        }

        @Test
        @DisplayName("deve adicionar ItemServicoOS à lista de itens de serviço")
        void deveAdicionarItemServicoOS() {
            // Given
            ItemServicoOS itemServico = ItemServicoOSBuilder.itemServicoOS(os, servico).build();

            // When
            os.adicionarItemServico(itemServico);

            // Then
            assertThat(os.getItensServico()).hasSize(1);
            assertThat(os.getItensServico()).contains(itemServico);
            assertThat(itemServico.getOrdemServico()).isEqualTo(os); // Verifica o relacionamento bidirecional
        }

        @Test
        @DisplayName("deve adicionar ItemPecaOS à lista de itens de peça")
        void deveAdicionarItemPecaOS() {
            // Given
            ItemPecaOS itemPeca = ItemPecaOSBuilder.itemPecaOS(os, peca).build();

            // When
            os.adicionarItemPeca(itemPeca);

            // Then
            assertThat(os.getItensPeca()).hasSize(1);
            assertThat(os.getItensPeca()).contains(itemPeca);
            assertThat(itemPeca.getOrdemServico()).isEqualTo(os); // Verifica o relacionamento bidirecional
        }

        @Test
        @DisplayName("deve adicionar Orcamento e definir a versão corretamente (primeiro orçamento)")
        void deveAdicionarOrcamentoEDefinirVersaoCorretamentePrimeiro() {
            // Given
            Orcamento orcamento = OrcamentoBuilder.orcamento(os).versao(null).build(); // Versão será definida pelo método

            // When
            os.adicionarOrcamento(orcamento);

            // Then
            assertThat(os.getOrcamentos()).hasSize(1);
            assertThat(os.getOrcamentos()).contains(orcamento);
            assertThat(orcamento.getOrdemServico()).isEqualTo(os); // Verifica o relacionamento bidirecional
            assertThat(orcamento.getVersao()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve adicionar Orcamento e definir a versão corretamente (orçamentos subsequentes)")
        void deveAdicionarOrcamentoEDefinirVersaoCorretamenteSubsequente() {
            // Given
            Orcamento orcamento1 = OrcamentoBuilder.orcamento(os).versao(1).build();
            os.adicionarOrcamento(orcamento1);

            Orcamento orcamento2 = OrcamentoBuilder.orcamento(os).versao(null).build(); // Versão será definida pelo método

            // When
            os.adicionarOrcamento(orcamento2);

            // Then
            assertThat(os.getOrcamentos()).hasSize(2);
            assertThat(os.getOrcamentos()).contains(orcamento1, orcamento2);
            assertThat(orcamento1.getVersao()).isEqualTo(1); // Versão do primeiro não deve mudar
            assertThat(orcamento2.getVersao()).isEqualTo(2);
        }

        @Test
        @DisplayName("getOrcamentoAprovado deve retornar o orçamento com status APROVADA")
        void getOrcamentoAprovadoDeveRetornarOrcamentoAprovado() {
            // Given
            Orcamento orcamentoRascunho = OrcamentoBuilder.orcamento(os).status(StatusOrcamento.RASCUNHO).build();
            Orcamento orcamentoAprovado = OrcamentoBuilder.orcamento(os).status(StatusOrcamento.APROVADA).build();
            Orcamento orcamentoEnviado = OrcamentoBuilder.orcamento(os).status(StatusOrcamento.ENVIADA).build();

            os.getOrcamentos().addAll(List.of(orcamentoRascunho, orcamentoAprovado, orcamentoEnviado));

            // When
            Optional<Orcamento> resultado = os.getOrcamentoAprovado();

            // Then
            assertThat(resultado).isPresent();
            assertThat(resultado.get()).isEqualTo(orcamentoAprovado);
        }

        @Test
        @DisplayName("getOrcamentoAprovado deve retornar Optional vazio se nenhum orçamento aprovado")
        void getOrcamentoAprovadoDeveRetornarOptionalVazioSeNenhumAprovado() {
            // Given
            Orcamento orcamentoRascunho = OrcamentoBuilder.orcamento(os).status(StatusOrcamento.RASCUNHO).build();
            Orcamento orcamentoEnviado = OrcamentoBuilder.orcamento(os).status(StatusOrcamento.ENVIADA).build();

            os.getOrcamentos().addAll(List.of(orcamentoRascunho, orcamentoEnviado));

            // When
            Optional<Orcamento> resultado = os.getOrcamentoAprovado();

            // Then
            assertThat(resultado).isNotPresent();
        }

        @Test
        @DisplayName("getOrcamentoVigente deve retornar o orçamento com a maior versão")
        void getOrcamentoVigenteDeveRetornarOrcamentoComMaiorVersao() {
            // Given
            Orcamento orcamentoV1 = OrcamentoBuilder.orcamento(os).versao(1).status(StatusOrcamento.CANCELADA).build();
            Orcamento orcamentoV2 = OrcamentoBuilder.orcamento(os).versao(2).status(StatusOrcamento.ENVIADA).build();
            Orcamento orcamentoV3 = OrcamentoBuilder.orcamento(os).versao(3).status(StatusOrcamento.RASCUNHO).build();

            os.getOrcamentos().addAll(List.of(orcamentoV1, orcamentoV2, orcamentoV3));

            // When
            Optional<Orcamento> resultado = os.getOrcamentoVigente();

            // Then
            assertThat(resultado).isPresent();
            assertThat(resultado.get()).isEqualTo(orcamentoV3);
        }

        @Test
        @DisplayName("getOrcamentoVigente deve retornar Optional vazio se não houver orçamentos")
        void getOrcamentoVigenteDeveRetornarOptionalVazioSeNaoHouverOrcamentos() {
            // Given (lista de orçamentos vazia por padrão)

            // When
            Optional<Orcamento> resultado = os.getOrcamentoVigente();

            // Then
            assertThat(resultado).isNotPresent();
        }
    }

    @Nested
    @DisplayName("Testes de Cálculo de Tempo")
    class CalculoTempoTests {

        private OrdemServico os;

        @BeforeEach
        void setupCalculoTempo() {
            os = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendenteRecepcao).build();
        }

        @Test
        @DisplayName("calcularTempoExecucaoMinutos deve retornar null se dataInicioExecucao é nula")
        void calcularTempoExecucaoMinutosDeveRetornarNullSeDataInicioExecucaoNula() {
            // Given
            os.setDataInicioExecucao(null);
            os.setDataFinalizacao(LocalDateTime.now());

            // When
            Long tempo = os.calcularTempoExecucaoMinutos();

            // Then
            assertThat(tempo).isNull();
        }

        @Test
        @DisplayName("calcularTempoExecucaoMinutos deve retornar null se dataFinalizacao é nula")
        void calcularTempoExecucaoMinutosDeveRetornarNullSeDataFinalizacaoNula() {
            // Given
            os.setDataInicioExecucao(LocalDateTime.now().minusHours(1));
            os.setDataFinalizacao(null);

            // When
            Long tempo = os.calcularTempoExecucaoMinutos();

            // Then
            assertThat(tempo).isNull();
        }

        @Test
        @DisplayName("calcularTempoExecucaoMinutos deve retornar a diferença em minutos quando ambas as datas estão presentes")
        void calcularTempoExecucaoMinutosDeveRetornarDiferencaEmMinutos() {
            // Given
            LocalDateTime inicio = LocalDateTime.of(2023, 1, 1, 10, 0, 0);
            LocalDateTime fim = LocalDateTime.of(2023, 1, 1, 11, 30, 0); // 90 minutos
            os.setDataInicioExecucao(inicio);
            os.setDataFinalizacao(fim);

            // When
            Long tempo = os.calcularTempoExecucaoMinutos();

            // Then
            assertThat(tempo).isEqualTo(90L);
        }

        @Test
        @DisplayName("calcularTempoExecucaoMinutos deve retornar zero se as datas são iguais")
        void calcularTempoExecucaoMinutosDeveRetornarZeroSeDatasIguais() {
            // Given
            LocalDateTime agora = LocalDateTime.now();
            os.setDataInicioExecucao(agora);
            os.setDataFinalizacao(agora);

            // When
            Long tempo = os.calcularTempoExecucaoMinutos();

            // Then
            assertThat(tempo).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Testes de Equals e HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("deve retornar true para objetos OrdemServico com o mesmo ID")
        void deveRetornarTrueParaObjetosOrdemServicoComMesmoID() {
            // Given
            UUID id = UUID.randomUUID();
            OrdemServico os1 = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendenteRecepcao).id(id).queixaCliente("A").build();
            OrdemServico os2 = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendenteRecepcao).id(id).queixaCliente("B").build(); // Queixa diferente, mas ID igual

            // Then
            assertThat(os1).isEqualTo(os2);
            assertThat(os1.hashCode()).isEqualTo(os2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para objetos OrdemServico com IDs diferentes")
        void deveRetornarFalseParaObjetosOrdemServicoComIDsDiferentes() {
            // Given
            OrdemServico os1 = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendenteRecepcao).id(UUID.randomUUID()).build();
            OrdemServico os2 = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendenteRecepcao).id(UUID.randomUUID()).build();

            // Then
            assertThat(os1).isNotEqualTo(os2);
            assertThat(os1.hashCode()).isNotEqualTo(os2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para OrdemServico e objeto nulo")
        void deveRetornarFalseParaOrdemServicoEObjetoNulo() {
            // Given
            OrdemServico os = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendenteRecepcao).build();

            // Then
            assertThat(os).isNotEqualTo(null);
        }

        @Test
        @DisplayName("deve retornar false para OrdemServico e objeto de classe diferente")
        void deveRetornarFalseParaOrdemServicoEObjetoDeClasseDiferente() {
            // Given
            OrdemServico os = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendenteRecepcao).build();
            Object obj = new Object();

            // Then
            assertThat(os).isNotEqualTo(obj);
        }
    }
}
