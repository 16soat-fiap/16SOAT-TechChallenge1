package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.StatusItemOS;
import com.autopecas.autopecas.util.test.ClienteBuilder;
import com.autopecas.autopecas.util.test.FuncionarioBuilder;
import com.autopecas.autopecas.util.test.ItemPecaOSBuilder;
import com.autopecas.autopecas.util.test.OrdemServicoBuilder;
import com.autopecas.autopecas.util.test.PecaBuilder;
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

@DisplayName("ItemPecaOS")
class ItemPecaOSTest {

    private OrdemServico ordemServico;
    private Peca peca;
    private Mecanico mecanico;

    @BeforeEach
    void setUp() {
        Cliente cliente = ClienteBuilder.clientePF().build();
        Veiculo veiculo = VeiculoBuilder.veiculo(cliente).build();
        Atendente atendente = FuncionarioBuilder.atendente().build();
        ordemServico = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendente).build();
        peca = PecaBuilder.peca().precoVenda(new BigDecimal("50.00")).build();
        mecanico = FuncionarioBuilder.mecanico().build();
    }

    @Nested
    @DisplayName("Testes de Construtor e Builder")
    class ConstrutorBuilderTests {

        @Test
        @DisplayName("deve criar ItemPecaOS com builder e valores padrão do builder")
        void deveCriarItemPecaOSComBuilderEValoresPadrao() {
            // Given
            ItemPecaOS item = ItemPecaOSBuilder.itemPecaOS(ordemServico, peca).build();

            // Then
            assertThat(item).isNotNull();
            assertThat(item.getId()).isNull(); // Gerado pela persistência
            assertThat(item.getOrdemServico()).isEqualTo(ordemServico);
            assertThat(item.getPeca()).isEqualTo(peca);
            assertThat(item.getQuantidade()).isEqualTo(1);
            assertThat(item.getPrecoUnitario()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.PENDENTE);
            assertThat(item.getInstaladoPor()).isNull();
            assertThat(item.getDataInstalacao()).isNull();
        }

        @Test
        @DisplayName("deve criar ItemPecaOS com builder e todos os valores fornecidos")
        void deveCriarItemPecaOSComBuilderEValoresFornecidos() {
            // Given
            Long id = 1L;
            Integer quantidade = 2;
            BigDecimal precoUnitario = new BigDecimal("60.00");
            StatusItemOS status = StatusItemOS.CONCLUIDO;
            LocalDateTime dataInstalacao = LocalDateTime.now().minusHours(1);

            // When
            ItemPecaOS item = ItemPecaOS.builder()
                    .id(id)
                    .ordemServico(ordemServico)
                    .peca(peca)
                    .quantidade(quantidade)
                    .precoUnitario(precoUnitario)
                    .status(status)
                    .instaladoPor(mecanico)
                    .dataInstalacao(dataInstalacao)
                    .build();

            // Then
            assertThat(item).isNotNull();
            assertThat(item.getId()).isEqualTo(id);
            assertThat(item.getOrdemServico()).isEqualTo(ordemServico);
            assertThat(item.getPeca()).isEqualTo(peca);
            assertThat(item.getQuantidade()).isEqualTo(quantidade);
            assertThat(item.getPrecoUnitario()).isEqualByComparingTo(precoUnitario);
            assertThat(item.getStatus()).isEqualTo(status);
            assertThat(item.getInstaladoPor()).isEqualTo(mecanico);
            assertThat(item.getDataInstalacao()).isEqualTo(dataInstalacao);
        }
    }

    @Nested
    @DisplayName("Testes do método calcularSubtotal()")
    class CalcularSubtotalTests {

        @Test
        @DisplayName("deve calcular subtotal corretamente")
        void deveCalcularSubtotalCorretamente() {
            // Given
            ItemPecaOS item = ItemPecaOSBuilder.itemPecaOS(ordemServico, peca, 3, new BigDecimal("25.00")).build();

            // When
            BigDecimal subtotal = item.calcularSubtotal();

            // Then
            assertThat(subtotal).isEqualByComparingTo(new BigDecimal("75.00")); // 3 * 25.00
        }

        @Test
        @DisplayName("deve calcular subtotal com quantidade 1")
        void deveCalcularSubtotalComQuantidadeUm() {
            // Given
            ItemPecaOS item = ItemPecaOSBuilder.itemPecaOS(ordemServico, peca, 1, new BigDecimal("40.00")).build();

            // When
            BigDecimal subtotal = item.calcularSubtotal();

            // Then
            assertThat(subtotal).isEqualByComparingTo(new BigDecimal("40.00")); // 1 * 40.00
        }

        @Test
        @DisplayName("deve calcular subtotal com preço unitário zero")
        void deveCalcularSubtotalComPrecoUnitarioZero() {
            // Given
            ItemPecaOS item = ItemPecaOSBuilder.itemPecaOS(ordemServico, peca, 5, BigDecimal.ZERO).build();

            // When
            BigDecimal subtotal = item.calcularSubtotal();

            // Then
            assertThat(subtotal).isEqualByComparingTo(BigDecimal.ZERO); // 5 * 0.00
        }
    }

    @Nested
    @DisplayName("Testes dos métodos de transição de status")
    class TransicaoStatusTests {

        private ItemPecaOS item;

        @BeforeEach
        void setupTransicaoStatus() {
            item = ItemPecaOSBuilder.itemPecaOS(ordemServico, peca).build();
        }

        @Test
        @DisplayName("instalar deve mudar status para CONCLUIDO e registrar mecanico e data")
        void instalarDeveMudarStatusERegistrarDados() {
            // Given
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.PENDENTE);
            assertThat(item.getDataInstalacao()).isNull();
            assertThat(item.getInstaladoPor()).isNull();

            // When
            item.instalar(mecanico);

            // Then
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.CONCLUIDO);
            assertThat(item.getDataInstalacao()).isNotNull();
            assertThat(item.getInstaladoPor()).isEqualTo(mecanico);
        }

        @Test
        @DisplayName("instalar deve lançar IllegalStateException se status não for PENDENTE")
        void instalarDeveLancarExcecaoSeStatusNaoPendente() {
            // Given
            item.setStatus(StatusItemOS.CANCELADO);

            // When / Then
            assertThatThrownBy(() -> item.instalar(mecanico))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Apenas itens pendentes podem ser atualizados. Atual: " + StatusItemOS.CANCELADO);
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.CANCELADO); // Status não deve mudar
            assertThat(item.getDataInstalacao()).isNull();
            assertThat(item.getInstaladoPor()).isNull();
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
                    .hasMessageContaining("Status inválido para essa ação. Nesse caso usar Devolução");
            assertThat(item.getStatus()).isEqualTo(StatusItemOS.CONCLUIDO); // Status não deve mudar
        }
    }

    @Nested
    @DisplayName("Testes de Equals e HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("deve retornar true para objetos ItemPecaOS com o mesmo ID")
        void deveRetornarTrueParaObjetosItemPecaOSComMesmoID() {
            // Given
            Long id = 1L;
            ItemPecaOS item1 = ItemPecaOSBuilder.itemPecaOS(ordemServico, peca).id(id).quantidade(1).build();
            ItemPecaOS item2 = ItemPecaOSBuilder.itemPecaOS(ordemServico, peca).id(id).quantidade(2).build(); // Quantidade diferente, mas ID igual

            // Then
            assertThat(item1).isEqualTo(item2);
            assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para objetos ItemPecaOS com IDs diferentes")
        void deveRetornarFalseParaObjetosItemPecaOSComIDsDiferentes() {
            // Given
            ItemPecaOS item1 = ItemPecaOSBuilder.itemPecaOS(ordemServico, peca).id(1L).build();
            ItemPecaOS item2 = ItemPecaOSBuilder.itemPecaOS(ordemServico, peca).id(2L).build();

            // Then
            assertThat(item1).isNotEqualTo(item2);
            assertThat(item1.hashCode()).isNotEqualTo(item2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para ItemPecaOS e objeto nulo")
        void deveRetornarFalseParaItemPecaOSEObjetoNulo() {
            // Given
            ItemPecaOS item = ItemPecaOSBuilder.itemPecaOS(ordemServico, peca).build();

            // Then
            assertThat(item).isNotEqualTo(null);
        }

        @Test
        @DisplayName("deve retornar false para ItemPecaOS e objeto de classe diferente")
        void deveRetornarFalseParaItemPecaOSEObjetoDeClasseDiferente() {
            // Given
            ItemPecaOS item = ItemPecaOSBuilder.itemPecaOS(ordemServico, peca).build();
            Object obj = new Object();

            // Then
            assertThat(item).isNotEqualTo(obj);
        }
    }
}
