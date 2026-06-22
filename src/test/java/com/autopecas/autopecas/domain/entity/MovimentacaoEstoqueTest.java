package com.autopecas.autopecas.domain.entity;
import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.util.test.MovimentacaoEstoqueBuilder;
import com.autopecas.autopecas.util.test.PecaBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
@DisplayName("MovimentacaoEstoque")
class MovimentacaoEstoqueTest {
    @Nested
    @DisplayName("Construção via builder")
    class ConstrucaoViaBuilder {
        @Test
        @DisplayName("deve criar MovimentacaoEstoque de ENTRADA com valores do builder")
        void deveCriarMovimentacaoEntradaComValoresPadrao() {
            // Given
            Peca peca = PecaBuilder.peca().build();
            // When
            MovimentacaoEstoque mov = MovimentacaoEstoqueBuilder.movimentacaoEstoque(peca).build();
            // Then
            assertThat(mov).isNotNull();
            assertThat(mov.getId()).isNotNull();
            assertThat(mov.getPeca()).isEqualTo(peca);
            assertThat(mov.getTipo()).isEqualTo(TipoMovimentacaoEstoque.ENTRADA);
            assertThat(mov.getQuantidade()).isEqualTo(10);
            assertThat(mov.getSaldoApos()).isEqualTo(10);
            assertThat(mov.getValorUnitarioMomento()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(mov.getMotivo()).isEqualTo("Compra de reposicao");
            assertThat(mov.getOrdemServico()).isNull();
            assertThat(mov.getExecutadoPor()).isNull();
            assertThat(mov.getCreatedAt()).isNull(); // Gerado pela persistência
        }
        @Test
        @DisplayName("deve criar MovimentacaoEstoque de SAIDA com valores customizados")
        void deveCriarMovimentacaoSaidaComValoresCustomizados() {
            // Given
            Peca peca = PecaBuilder.peca().build();
            UUID id = UUID.randomUUID();
            // When
            MovimentacaoEstoque mov = MovimentacaoEstoqueBuilder.movimentacaoSaida(peca)
                    .id(id)
                    .motivo("Uso em OS-000042")
                    .build();
            // Then
            assertThat(mov.getId()).isEqualTo(id);
            assertThat(mov.getTipo()).isEqualTo(TipoMovimentacaoEstoque.SAIDA);
            assertThat(mov.getQuantidade()).isEqualTo(3);
            assertThat(mov.getSaldoApos()).isEqualTo(7);
            assertThat(mov.getMotivo()).isEqualTo("Uso em OS-000042");
        }
        @Test
        @DisplayName("deve criar MovimentacaoEstoque com motivo nulo")
        void deveCriarMovimentacaoSemMotivo() {
            Peca peca = PecaBuilder.peca().build();
            MovimentacaoEstoque mov = MovimentacaoEstoqueBuilder.movimentacaoEstoque(peca)
                    .motivo(null)
                    .build();
            assertThat(mov.getMotivo()).isNull();
        }
    }
    @Nested
    @DisplayName("Equals e HashCode via @EqualsAndHashCode(of = id)")
    class EqualsHashCode {
        @Test
        @DisplayName("deve retornar true para movimentações com o mesmo ID")
        void deveRetornarTrueParaMovimentacoesComMesmoId() {
            // Given
            Peca peca = PecaBuilder.peca().build();
            UUID id = UUID.randomUUID();
            MovimentacaoEstoque mov1 = MovimentacaoEstoqueBuilder.movimentacaoEstoque(peca).id(id).build();
            MovimentacaoEstoque mov2 = MovimentacaoEstoqueBuilder.movimentacaoSaida(peca).id(id).build();
            // Then
            assertThat(mov1).isEqualTo(mov2);
            assertThat(mov1.hashCode()).isEqualTo(mov2.hashCode());
        }
        @Test
        @DisplayName("deve retornar false para movimentações com IDs diferentes")
        void deveRetornarFalseParaMovimentacoesComIdsDiferentes() {
            Peca peca = PecaBuilder.peca().build();
            MovimentacaoEstoque mov1 = MovimentacaoEstoqueBuilder.movimentacaoEstoque(peca).build();
            MovimentacaoEstoque mov2 = MovimentacaoEstoqueBuilder.movimentacaoEstoque(peca).build();
            assertThat(mov1).isNotEqualTo(mov2);
        }
    }
}
