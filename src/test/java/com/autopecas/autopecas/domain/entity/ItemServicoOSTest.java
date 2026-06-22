package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.StatusItemOS;
import com.autopecas.autopecas.util.test.ClienteBuilder;
import com.autopecas.autopecas.util.test.FuncionarioBuilder;
import com.autopecas.autopecas.util.test.ItemServicoOSBuilder;
import com.autopecas.autopecas.util.test.OrdemServicoBuilder;
import com.autopecas.autopecas.util.test.ServicoBuilder;
import com.autopecas.autopecas.util.test.VeiculoBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ItemServicoOS")
class ItemServicoOSTest {

    private OrdemServico ordemServico;
    private Servico servico;
    private Mecanico mecanico;

    @BeforeEach
    void setUp() {
        Cliente cliente = ClienteBuilder.clientePF().build();
        Veiculo veiculo = VeiculoBuilder.veiculo(cliente).build();
        Atendente atendente = FuncionarioBuilder.atendente().build();
        ordemServico = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendente).build();
        servico = ServicoBuilder.servico().precoBase(new BigDecimal("100.00")).build();
        mecanico = FuncionarioBuilder.mecanico().build();
    }

    @Nested
    @DisplayName("Testes de Construtor e Builder")
    class ConstrutorBuilderTests {

        @Test
        @DisplayName("deve criar ItemServicoOS com builder e valores padrão do builder")
        void deveCriarItemServicoOSComBuilderEValoresPadrao() {
            // Given
            ItemServicoOS item = ItemServicoOSBuilder.itemServicoOS(ordemServico, servico).build();

            // Then
            assertThat(item).isNotNull();
            assertThat(item.getId()).isNull(); // Gerado pela persistência
            assertThat(item.getOrdemServico()).isEqualTo(ordemServico);
            assertThat(item.getServico()).isEqualTo(servico);
            assertThat(item.getQuantidade()).isEqualTo(1);
            assertThat(item.getPrecoUnitario()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.PENDENTE);
            assertThat(item.getExecutadoPor()).isNull();
            assertThat(item.getDataInicioExecucao()).isNull();
            assertThat(item.getDataFimExecucao()).isNull();
            assertThat(item.getObservacao()).isEqualTo("Serviço a ser executado");
        }

        @Test
        @DisplayName("deve criar ItemServicoOS com builder e todos os valores fornecidos")
        void deveCriarItemServicoOSComBuilderEValoresFornecidos() {
            // Given
            Long id = 1L;
            Integer quantidade = 2;
            BigDecimal precoUnitario = new BigDecimal("120.00");
            StatusItemOS status = StatusItemOS.CONCLUIDO;
            LocalDateTime dataInicio = LocalDateTime.now().minusHours(2);
            LocalDateTime dataFim = LocalDateTime.now().minusHours(1);
            String observacao = "Serviço concluído";

            // When
            ItemServicoOS item = ItemServicoOS.builder()
                    .id(id)
                    .ordemServico(ordemServico)
                    .servico(servico)
                    .quantidade(quantidade)
                    .precoUnitario(precoUnitario)
                    .status(status)
                    .executadoPor(mecanico)
                    .dataInicioExecucao(dataInicio)
                    .dataFimExecucao(dataFim)
                    .observacao(observacao)
                    .build();

            // Then
            assertThat(item).isNotNull();
            assertThat(item.getId()).isEqualTo(id);
            assertThat(item.getOrdemServico()).isEqualTo(ordemServico);
            assertThat(item.getServico()).isEqualTo(servico);
            assertThat(item.getQuantidade()).isEqualTo(quantidade);
            assertThat(item.getPrecoUnitario()).isEqualByComparingTo(precoUnitario);
            assertThat(item.getStatus()).isEqualTo(status);
            assertThat(item.getExecutadoPor()).isEqualTo(mecanico);
            assertThat(item.getDataInicioExecucao()).isEqualTo(dataInicio);
            assertThat(item.getDataFimExecucao()).isEqualTo(dataFim);
            assertThat(item.getObservacao()).isEqualTo(observacao);
        }
    }

    @Nested
    @DisplayName("Testes do método calcularSubtotal()")
    class CalcularSubtotalTests {

        @Test
        @DisplayName("deve calcular subtotal corretamente")
        void deveCalcularSubtotalCorretamente() {
            // Given
            ItemServicoOS item = ItemServicoOSBuilder.itemServicoOS(ordemServico, servico, 3, new BigDecimal("50.00"), null).build();

            // When
            BigDecimal subtotal = item.calcularSubtotal();

            // Then
            assertThat(subtotal).isEqualByComparingTo(new BigDecimal("150.00")); // 3 * 50.00
        }

        @Test
        @DisplayName("deve calcular subtotal com quantidade 1")
        void deveCalcularSubtotalComQuantidadeUm() {
            // Given
            ItemServicoOS item = ItemServicoOSBuilder.itemServicoOS(ordemServico, servico, 1, new BigDecimal("75.00"), null).build();

            // When
            BigDecimal subtotal = item.calcularSubtotal();

            // Then
            assertThat(subtotal).isEqualByComparingTo(new BigDecimal("75.00")); // 1 * 75.00
        }

        @Test
        @DisplayName("deve calcular subtotal com preço unitário zero")
        void deveCalcularSubtotalComPrecoUnitarioZero() {
            // Given
            ItemServicoOS item = ItemServicoOSBuilder.itemServicoOS(ordemServico, servico, 5, BigDecimal.ZERO, null).build();

            // When
            BigDecimal subtotal = item.calcularSubtotal();

            // Then
            assertThat(subtotal).isEqualByComparingTo(BigDecimal.ZERO); // 5 * 0.00
        }
    }

    @Nested
    @DisplayName("Testes dos métodos de transição de status")
    class TransicaoStatusTests {

        private ItemServicoOS item;

        @BeforeEach
        void setupTransicaoStatus() {
            item = ItemServicoOSBuilder.itemServicoOS(ordemServico, servico).build();
        }

        @Test
        @DisplayName("iniciarExecucao deve mudar status para EM_EXECUCAO e registrar data/mecanico")
        void iniciarExecucaoDeveMudarStatusERegistrarDados() {
            // Given
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.PENDENTE);
            assertThat(item.getDataInicioExecucao()).isNull();
            assertThat(item.getExecutadoPor()).isNull();

            // When
            item.iniciarExecucao(mecanico);

            // Then
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.EM_EXECUCAO);
            assertThat(item.getDataInicioExecucao()).isNotNull();
            assertThat(item.getExecutadoPor()).isEqualTo(mecanico);
        }

        @Test
        @DisplayName("iniciarExecucao deve lançar IllegalStateException se status não for PENDENTE")
        void iniciarExecucaoDeveLancarExcecaoSeStatusNaoPendente() {
            // Given
            item.setStatus(StatusItemOS.CONCLUIDO);

            // When / Then
            assertThatThrownBy(() -> item.iniciarExecucao(mecanico))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Apenas itens pendentes podem ser alterados. Atual: " + StatusItemOS.CONCLUIDO);
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.CONCLUIDO); // Status não deve mudar
            assertThat(item.getDataInicioExecucao()).isNull();
            assertThat(item.getExecutadoPor()).isNull();
        }

        @Test
        @DisplayName("concluir deve mudar status para CONCLUIDO e registrar data")
        void concluirDeveMudarStatusERegistrarData() {
            // Given
            item.iniciarExecucao(mecanico); // Primeiro inicia a execução
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.EM_EXECUCAO);
            assertThat(item.getDataFimExecucao()).isNull();

            // When
            item.concluir();

            // Then
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.CONCLUIDO);
            assertThat(item.getDataFimExecucao()).isNotNull();
        }

        @Test
        @DisplayName("concluir deve lançar IllegalStateException se status não for EM_EXECUCAO")
        void concluirDeveLancarExcecaoSeStatusNaoEmExecucao() {
            // Given
            item.setStatus(StatusItemOS.PENDENTE);

            // When / Then
            assertThatThrownBy(() -> item.concluir())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Apenas itens EM_EXECUCAO podem ser concluídos. Atual: " + StatusItemOS.PENDENTE);
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.PENDENTE); // Status não deve mudar
            assertThat(item.getDataFimExecucao()).isNull();
        }

        @Test
        @DisplayName("cancelar deve mudar status para CANCELADO se não for CONCLUIDO")
        void cancelarDeveMudarStatusParaCancelado() {
            // Given
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.PENDENTE);

            // When
            item.cancelar();

            // Then
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.CANCELADO);
        }

        @Test
        @DisplayName("cancelar deve lançar IllegalStateException se status for CONCLUIDO")
        void cancelarDeveLancarExcecaoSeStatusConcluido() {
            // Given
            item.setStatus(StatusItemOS.CONCLUIDO);

            // When / Then
            assertThatThrownBy(() -> item.cancelar())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Não é possível cancelar itens concluídos");
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.CONCLUIDO); // Status não deve mudar
        }
    }

    @Nested
    @DisplayName("Testes do método calcularTempoDeExecucaoMinutos()")
    class CalcularTempoExecucaoTests {

        private ItemServicoOS item;

        @BeforeEach
        void setupTempoExecucao() {
            item = ItemServicoOSBuilder.itemServicoOS(ordemServico, servico).build();
        }

        @Test
        @DisplayName("deve retornar null se dataInicioExecucao é nula")
        void deveRetornarNullSeDataInicioExecucaoNula() {
            // Given
            item.setDataInicioExecucao(null);
            item.setDataFimExecucao(LocalDateTime.now());

            // When
            Long tempo = item.calcularTempoDeExecucaoMinutos();

            // Then
            assertThat(tempo).isNull();
        }

        @Test
        @DisplayName("deve retornar null se dataFimExecucao é nula")
        void deveRetornarNullSeDataFimExecucaoNula() {
            // Given
            item.setDataInicioExecucao(LocalDateTime.now().minusHours(1));
            item.setDataFimExecucao(null);

            // When
            Long tempo = item.calcularTempoDeExecucaoMinutos();

            // Then
            assertThat(tempo).isNull();
        }

        @Test
        @DisplayName("deve retornar a diferença em minutos quando ambas as datas estão presentes")
        void deveRetornarDiferencaEmMinutos() {
            // Given
            LocalDateTime inicio = LocalDateTime.of(2023, 1, 1, 10, 0, 0);
            LocalDateTime fim = LocalDateTime.of(2023, 1, 1, 11, 30, 0); // 90 minutos
            item.setDataInicioExecucao(inicio);
            item.setDataFimExecucao(fim);

            // When
            Long tempo = item.calcularTempoDeExecucaoMinutos();

            // Then
            assertThat(tempo).isEqualTo(90L);
        }

        @Test
        @DisplayName("deve retornar zero se as datas de inicio e fim são iguais")
        void deveRetornarZeroSeDatasIguais() {
            // Given
            LocalDateTime agora = LocalDateTime.now();
            item.setDataInicioExecucao(agora);
            item.setDataFimExecucao(agora);

            // When
            Long tempo = item.calcularTempoDeExecucaoMinutos();

            // Then
            assertThat(tempo).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Testes de Equals e HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("deve retornar true para objetos ItemServicoOS com o mesmo ID")
        void deveRetornarTrueParaObjetosItemServicoOSComMesmoID() {
            // Given
            Long id = 1L;
            ItemServicoOS item1 = ItemServicoOSBuilder.itemServicoOS(ordemServico, servico).id(id).quantidade(1).build();
            ItemServicoOS item2 = ItemServicoOSBuilder.itemServicoOS(ordemServico, servico).id(id).quantidade(2).build(); // Quantidade diferente, mas ID igual

            // Then
            assertThat(item1).isEqualTo(item2);
            assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para objetos ItemServicoOS com IDs diferentes")
        void deveRetornarFalseParaObjetosItemServicoOSComIDsDiferentes() {
            // Given
            ItemServicoOS item1 = ItemServicoOSBuilder.itemServicoOS(ordemServico, servico).id(1L).build();
            ItemServicoOS item2 = ItemServicoOSBuilder.itemServicoOS(ordemServico, servico).id(2L).build();

            // Then
            assertThat(item1).isNotEqualTo(item2);
            assertThat(item1.hashCode()).isNotEqualTo(item2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para ItemServicoOS e objeto nulo")
        void deveRetornarFalseParaItemServicoOSEObjetoNulo() {
            // Given
            ItemServicoOS item = ItemServicoOSBuilder.itemServicoOS(ordemServico, servico).build();

            // Then
            assertThat(item).isNotEqualTo(null);
        }

        @Test
        @DisplayName("deve retornar false para ItemServicoOS e objeto de classe diferente")
        void deveRetornarFalseParaItemServicoOSEObjetoDeClasseDiferente() {
            // Given
            ItemServicoOS item = ItemServicoOSBuilder.itemServicoOS(ordemServico, servico).build();
            Object obj = new Object();

            // Then
            assertThat(item).isNotEqualTo(obj);
        }
    }
}
