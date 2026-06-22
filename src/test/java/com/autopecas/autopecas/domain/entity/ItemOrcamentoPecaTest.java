package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.util.test.ClienteBuilder;
import com.autopecas.autopecas.util.test.FuncionarioBuilder;
import com.autopecas.autopecas.util.test.ItemOrcamentoPecaBuilder;
import com.autopecas.autopecas.util.test.OrcamentoBuilder;
import com.autopecas.autopecas.util.test.OrdemServicoBuilder;
import com.autopecas.autopecas.util.test.PecaBuilder;
import com.autopecas.autopecas.util.test.VeiculoBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ItemOrcamentoPeca")
class ItemOrcamentoPecaTest {

    private Orcamento orcamento;
    private Peca peca;

    @BeforeEach
    void setUp() {
        Cliente cliente = ClienteBuilder.clientePF().build();
        Veiculo veiculo = VeiculoBuilder.veiculo(cliente).build();
        Atendente atendente = FuncionarioBuilder.atendente().build();
        OrdemServico ordemServico = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendente).build();
        orcamento = OrcamentoBuilder.orcamento(ordemServico).build();
        peca = PecaBuilder.peca().precoVenda(new BigDecimal("50.00")).build();
    }

    @Nested
    @DisplayName("Testes de Construtor e Builder")
    class ConstrutorBuilderTests {

        @Test
        @DisplayName("deve criar ItemOrcamentoPeca com builder e valores padrão do builder")
        void deveCriarItemOrcamentoPecaComBuilderEValoresPadrao() {
            // Given
            ItemOrcamentoPeca item = ItemOrcamentoPecaBuilder.itemOrcamentoPeca(orcamento, peca).build();

            // Then
            assertThat(item).isNotNull();
            assertThat(item.getId()).isNull(); // Gerado pela persistência
            assertThat(item.getOrcamento()).isEqualTo(orcamento);
            assertThat(item.getPeca()).isEqualTo(peca);
            assertThat(item.getQuantidade()).isEqualTo(1);
            assertThat(item.getPrecoUnitario()).isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("deve criar ItemOrcamentoPeca com builder e todos os valores fornecidos")
        void deveCriarItemOrcamentoPecaComBuilderEValoresFornecidos() {
            // Given
            Long id = 1L;
            Integer quantidade = 2;
            BigDecimal precoUnitario = new BigDecimal("60.00");

            // When
            ItemOrcamentoPeca item = ItemOrcamentoPeca.builder()
                    .id(id)
                    .orcamento(orcamento)
                    .peca(peca)
                    .quantidade(quantidade)
                    .precoUnitario(precoUnitario)
                    .build();

            // Then
            assertThat(item).isNotNull();
            assertThat(item.getId()).isEqualTo(id);
            assertThat(item.getOrcamento()).isEqualTo(orcamento);
            assertThat(item.getPeca()).isEqualTo(peca);
            assertThat(item.getQuantidade()).isEqualTo(quantidade);
            assertThat(item.getPrecoUnitario()).isEqualByComparingTo(precoUnitario);
        }
    }

    @Nested
    @DisplayName("Testes do método calcularSubtotal()")
    class CalcularSubtotalTests {

        @Test
        @DisplayName("deve calcular subtotal corretamente")
        void deveCalcularSubtotalCorretamente() {
            // Given
            ItemOrcamentoPeca item = ItemOrcamentoPecaBuilder.itemOrcamentoPeca(orcamento, peca, 3, new BigDecimal("25.00")).build();

            // When
            BigDecimal subtotal = item.calcularSubtotal();

            // Then
            assertThat(subtotal).isEqualByComparingTo(new BigDecimal("75.00")); // 3 * 25.00
        }

        @Test
        @DisplayName("deve calcular subtotal com quantidade 1")
        void deveCalcularSubtotalComQuantidadeUm() {
            // Given
            ItemOrcamentoPeca item = ItemOrcamentoPecaBuilder.itemOrcamentoPeca(orcamento, peca, 1, new BigDecimal("40.00")).build();

            // When
            BigDecimal subtotal = item.calcularSubtotal();

            // Then
            assertThat(subtotal).isEqualByComparingTo(new BigDecimal("40.00")); // 1 * 40.00
        }

        @Test
        @DisplayName("deve calcular subtotal com preço unitário zero")
        void deveCalcularSubtotalComPrecoUnitarioZero() {
            // Given
            ItemOrcamentoPeca item = ItemOrcamentoPecaBuilder.itemOrcamentoPeca(orcamento, peca, 5, BigDecimal.ZERO).build();

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
        @DisplayName("deve retornar true para objetos ItemOrcamentoPeca com o mesmo ID")
        void deveRetornarTrueParaObjetosItemOrcamentoPecaComMesmoID() {
            // Given
            Long id = 1L;
            ItemOrcamentoPeca item1 = ItemOrcamentoPecaBuilder.itemOrcamentoPeca(orcamento, peca).id(id).quantidade(1).build();
            ItemOrcamentoPeca item2 = ItemOrcamentoPecaBuilder.itemOrcamentoPeca(orcamento, peca).id(id).quantidade(2).build(); // Quantidade diferente, mas ID igual

            // Then
            assertThat(item1).isEqualTo(item2);
            assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para objetos ItemOrcamentoPeca com IDs diferentes")
        void deveRetornarFalseParaObjetosItemOrcamentoPecaComIDsDiferentes() {
            // Given
            ItemOrcamentoPeca item1 = ItemOrcamentoPecaBuilder.itemOrcamentoPeca(orcamento, peca).id(1L).build();
            ItemOrcamentoPeca item2 = ItemOrcamentoPecaBuilder.itemOrcamentoPeca(orcamento, peca).id(2L).build();

            // Then
            assertThat(item1).isNotEqualTo(item2);
            assertThat(item1.hashCode()).isNotEqualTo(item2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para ItemOrcamentoPeca e objeto nulo")
        void deveRetornarFalseParaItemOrcamentoPecaEObjetoNulo() {
            // Given
            ItemOrcamentoPeca item = ItemOrcamentoPecaBuilder.itemOrcamentoPeca(orcamento, peca).build();

            // Then
            assertThat(item).isNotEqualTo(null);
        }

        @Test
        @DisplayName("deve retornar false para ItemOrcamentoPeca e objeto de classe diferente")
        void deveRetornarFalseParaItemOrcamentoPecaEObjetoDeClasseDiferente() {
            // Given
            ItemOrcamentoPeca item = ItemOrcamentoPecaBuilder.itemOrcamentoPeca(orcamento, peca).build();
            Object obj = new Object();

            // Then
            assertThat(item).isNotEqualTo(obj);
        }
    }
}
