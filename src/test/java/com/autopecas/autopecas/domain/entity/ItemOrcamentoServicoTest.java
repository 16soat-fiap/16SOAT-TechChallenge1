package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.util.test.ClienteBuilder;
import com.autopecas.autopecas.util.test.FuncionarioBuilder;
import com.autopecas.autopecas.util.test.ItemOrcamentoServicoBuilder;
import com.autopecas.autopecas.util.test.OrcamentoBuilder;
import com.autopecas.autopecas.util.test.OrdemServicoBuilder;
import com.autopecas.autopecas.util.test.ServicoBuilder;
import com.autopecas.autopecas.util.test.VeiculoBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ItemOrcamentoServico")
class ItemOrcamentoServicoTest {

    private Orcamento orcamento;
    private Servico servico;

    @BeforeEach
    void setUp() {
        Cliente cliente = ClienteBuilder.clientePF().build();
        Veiculo veiculo = VeiculoBuilder.veiculo(cliente).build();
        Atendente atendente = FuncionarioBuilder.atendente().build();
        OrdemServico ordemServico = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendente).build();
        orcamento = OrcamentoBuilder.orcamento(ordemServico).build();
        servico = ServicoBuilder.servico().precoBase(new BigDecimal("100.00")).build();
    }

    @Nested
    @DisplayName("Testes de Construtor e Builder")
    class ConstrutorBuilderTests {

        @Test
        @DisplayName("deve criar ItemOrcamentoServico com builder e valores padrão do builder")
        void deveCriarItemOrcamentoServicoComBuilderEValoresPadrao() {
            // Given
            ItemOrcamentoServico item = ItemOrcamentoServicoBuilder.itemOrcamentoServico(orcamento, servico).build();

            // Then
            assertThat(item).isNotNull();
            assertThat(item.getId()).isNull(); // Gerado pela persistência
            assertThat(item.getOrcamento()).isEqualTo(orcamento);
            assertThat(item.getServico()).isEqualTo(servico);
            assertThat(item.getQuantidade()).isEqualTo(1);
            assertThat(item.getPrecoUnitario()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(item.getObservacao()).isEqualTo("Serviço padrão");
        }

        @Test
        @DisplayName("deve criar ItemOrcamentoServico com builder e todos os valores fornecidos")
        void deveCriarItemOrcamentoServicoComBuilderEValoresFornecidos() {
            // Given
            Long id = 1L;
            Integer quantidade = 2;
            BigDecimal precoUnitario = new BigDecimal("120.00");
            String observacao = "Observação customizada";

            // When
            ItemOrcamentoServico item = ItemOrcamentoServico.builder()
                    .id(id)
                    .orcamento(orcamento)
                    .servico(servico)
                    .quantidade(quantidade)
                    .precoUnitario(precoUnitario)
                    .observacao(observacao)
                    .build();

            // Then
            assertThat(item).isNotNull();
            assertThat(item.getId()).isEqualTo(id);
            assertThat(item.getOrcamento()).isEqualTo(orcamento);
            assertThat(item.getServico()).isEqualTo(servico);
            assertThat(item.getQuantidade()).isEqualTo(quantidade);
            assertThat(item.getPrecoUnitario()).isEqualByComparingTo(precoUnitario);
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
            ItemOrcamentoServico item = ItemOrcamentoServicoBuilder.itemOrcamentoServico(orcamento, servico, 3, new BigDecimal("50.00"), null).build();

            // When
            BigDecimal subtotal = item.calcularSubtotal();

            // Then
            assertThat(subtotal).isEqualByComparingTo(new BigDecimal("150.00")); // 3 * 50.00
        }

        @Test
        @DisplayName("deve calcular subtotal com quantidade 1")
        void deveCalcularSubtotalComQuantidadeUm() {
            // Given
            ItemOrcamentoServico item = ItemOrcamentoServicoBuilder.itemOrcamentoServico(orcamento, servico, 1, new BigDecimal("75.00"), null).build();

            // When
            BigDecimal subtotal = item.calcularSubtotal();

            // Then
            assertThat(subtotal).isEqualByComparingTo(new BigDecimal("75.00")); // 1 * 75.00
        }

        @Test
        @DisplayName("deve calcular subtotal com preço unitário zero")
        void deveCalcularSubtotalComPrecoUnitarioZero() {
            // Given
            ItemOrcamentoServico item = ItemOrcamentoServicoBuilder.itemOrcamentoServico(orcamento, servico, 5, BigDecimal.ZERO, null).build();

            // When
            BigDecimal subtotal = item.calcularSubtotal();

            // Then
            assertThat(subtotal).isEqualByComparingTo(BigDecimal.ZERO); // 5 * 0.00
        }
    }

    @Nested
    @DisplayName("Testes de Equals e HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("deve retornar true para objetos ItemOrcamentoServico com o mesmo ID")
        void deveRetornarTrueParaObjetosItemOrcamentoServicoComMesmoID() {
            // Given
            Long id = 1L;
            ItemOrcamentoServico item1 = ItemOrcamentoServicoBuilder.itemOrcamentoServico(orcamento, servico).id(id).quantidade(1).build();
            ItemOrcamentoServico item2 = ItemOrcamentoServicoBuilder.itemOrcamentoServico(orcamento, servico).id(id).quantidade(2).build(); // Quantidade diferente, mas ID igual

            // Then
            assertThat(item1).isEqualTo(item2);
            assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para objetos ItemOrcamentoServico com IDs diferentes")
        void deveRetornarFalseParaObjetosItemOrcamentoServicoComIDsDiferentes() {
            // Given
            ItemOrcamentoServico item1 = ItemOrcamentoServicoBuilder.itemOrcamentoServico(orcamento, servico).id(1L).build();
            ItemOrcamentoServico item2 = ItemOrcamentoServicoBuilder.itemOrcamentoServico(orcamento, servico).id(2L).build();

            // Then
            assertThat(item1).isNotEqualTo(item2);
            assertThat(item1.hashCode()).isNotEqualTo(item2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para ItemOrcamentoServico e objeto nulo")
        void deveRetornarFalseParaItemOrcamentoServicoEObjetoNulo() {
            // Given
            ItemOrcamentoServico item = ItemOrcamentoServicoBuilder.itemOrcamentoServico(orcamento, servico).build();

            // Then
            assertThat(item).isNotEqualTo(null);
        }

        @Test
        @DisplayName("deve retornar false para ItemOrcamentoServico e objeto de classe diferente")
        void deveRetornarFalseParaItemOrcamentoServicoEObjetoDeClasseDiferente() {
            // Given
            ItemOrcamentoServico item = ItemOrcamentoServicoBuilder.itemOrcamentoServico(orcamento, servico).build();
            Object obj = new Object();

            // Then
            assertThat(item).isNotEqualTo(obj);
        }
    }
}
