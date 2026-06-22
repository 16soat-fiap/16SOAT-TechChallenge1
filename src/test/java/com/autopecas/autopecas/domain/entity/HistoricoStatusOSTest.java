package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.util.test.ClienteBuilder;
import com.autopecas.autopecas.util.test.FuncionarioBuilder;
import com.autopecas.autopecas.util.test.HistoricoStatusOSBuilder;
import com.autopecas.autopecas.util.test.VeiculoBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HistoricoStatusOS")
class HistoricoStatusOSTest {

    private OrdemServico ordemServico;
    private Atendente atendente;
    private Mecanico mecanico;

    @BeforeEach
    void setUp() {
        Cliente cliente = ClienteBuilder.clientePF().build();
        Veiculo veiculo = VeiculoBuilder.veiculo(cliente).build();
        atendente = FuncionarioBuilder.atendente().build();
        mecanico = FuncionarioBuilder.mecanico().build();

        ordemServico = OrdemServico.builder()
                .cliente(cliente)
                .veiculo(veiculo)
                .atendenteRecepcao(atendente)
                .numero("OS-000001")
                .status(StatusOS.RECEBIDA)
                .queixaCliente("Barulho estranho no motor")
                .build();
    }

    @Nested
    @DisplayName("Testes dos factory methods")
    class FactoryMethodsTests {

        @Test
        @DisplayName("abertura deve criar histórico inicial com status RECEBIDA e atendente responsável")
        void aberturaDeveCriarHistoricoInicialComStatusRecebidaEAtendenteResponsavel() {
            // Given

            // When
            HistoricoStatusOS historico = HistoricoStatusOSBuilder.abertura(ordemServico, atendente);

            // Then
            assertThat(historico).isNotNull();
            assertThat(historico.getId()).isNull();
            assertThat(historico.getOrdemServico()).isEqualTo(ordemServico);
            assertThat(historico.getStatusAnterior()).isNull();
            assertThat(historico.getStatusNovo()).isEqualTo(StatusOS.RECEBIDA);
            assertThat(historico.getObservacao()).isEqualTo("Ordem de serviço aberta.");
            assertThat(historico.getAlteradoPor()).isEqualTo(atendente.getIdentificacao() + " — " + atendente.getTipo());
            assertThat(historico.getExecutadoPor()).isEqualTo(atendente);
            assertThat(historico.getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("porFuncionario deve criar histórico com status anterior, novo status e funcionário executor")
        void porFuncionarioDeveCriarHistoricoComStatusAnteriorNovoStatusEFuncionarioExecutor() {
            // Given
            StatusOS statusAnterior = StatusOS.EM_DIAGNOSTICO;
            StatusOS novoStatus = StatusOS.AGUARDANDO_APROVACAO;

            // When
            HistoricoStatusOS historico = HistoricoStatusOSBuilder.porFuncionario(
                    ordemServico,
                    statusAnterior,
                    novoStatus,
                    mecanico
            );

            // Then
            assertThat(historico).isNotNull();
            assertThat(historico.getId()).isNull();
            assertThat(historico.getOrdemServico()).isEqualTo(ordemServico);
            assertThat(historico.getStatusAnterior()).isEqualTo(statusAnterior);
            assertThat(historico.getStatusNovo()).isEqualTo(novoStatus);
            assertThat(historico.getObservacao()).isEqualTo("Observação padrão do funcionário");
            assertThat(historico.getAlteradoPor()).isEqualTo(mecanico.getIdentificacao() + " — " + mecanico.getTipo());
            assertThat(historico.getExecutadoPor()).isEqualTo(mecanico);
            assertThat(historico.getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("porSistema deve criar histórico com executor nulo e alterado por SISTEMA")
        void porSistemaDeveCriarHistoricoComExecutorNuloEAlteradoPorSistema() {
            // Given
            StatusOS statusAnterior = StatusOS.AGUARDANDO_APROVACAO;
            StatusOS novoStatus = StatusOS.EM_EXECUCAO;

            // When
            HistoricoStatusOS historico = HistoricoStatusOSBuilder.porSistema(
                    ordemServico,
                    statusAnterior,
                    novoStatus
            );

            // Then
            assertThat(historico).isNotNull();
            assertThat(historico.getId()).isNull();
            assertThat(historico.getOrdemServico()).isEqualTo(ordemServico);
            assertThat(historico.getStatusAnterior()).isEqualTo(statusAnterior);
            assertThat(historico.getStatusNovo()).isEqualTo(novoStatus);
            assertThat(historico.getObservacao()).isEqualTo("Observação padrão do sistema");
            assertThat(historico.getAlteradoPor()).isEqualTo("SISTEMA");
            assertThat(historico.getExecutadoPor()).isNull();
            assertThat(historico.getCreatedAt()).isNull();
        }
    }
}

