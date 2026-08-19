package com.autopecas.autopecas.domain.model.estoque;

import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.EstoqueInsuficienteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testes do agregado Peca, com foco no controle de saldo de estoque. */
@DisplayName("Peca")
class PecaTest {

    private static Peca nova() {
        return Peca.criar("PEC001", "Pastilha de freio", "Dianteira", new BigDecimal("120.00"), 2, "un");
    }

    private static Peca comSaldo(int saldo, int minimo) {
        return Peca.reconstituir(UUID.randomUUID(), "PEC001", "Pastilha de freio", null, "Bosch",
                new BigDecimal("120.00"), saldo, minimo, "un", true,
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("deve nascer com saldo zero — o estoque inicial entra por movimentação")
        void deveNascerComSaldoZero() {
            Peca peca = nova();

            assertThat(peca.getQuantidadeEstoque()).isZero();
            assertThat(peca.isAtivo()).isTrue();
            assertThat(peca.isNovo()).isTrue();
        }

        @Test
        @DisplayName("deve exigir código, nome e preço")
        void deveExigirCamposObrigatorios() {
            assertThatThrownBy(() -> Peca.criar(null, "Nome", null, BigDecimal.TEN, 1, "un"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Código");

            assertThatThrownBy(() -> Peca.criar("PEC001", " ", null, BigDecimal.TEN, 1, "un"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Nome");

            assertThatThrownBy(() -> Peca.criar("PEC001", "Nome", null, null, 1, "un"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Preço");
        }
    }

    @Nested
    @DisplayName("Saldo de estoque")
    class SaldoDeEstoque {

        @ParameterizedTest(name = "saldo {0}, pedido {1} -> suficiente? {2}")
        @CsvSource({"10, 5, true", "10, 10, true", "10, 11, false", "0, 1, false", "0, 0, true"})
        @DisplayName("deve avaliar suficiência de saldo")
        void deveAvaliarSuficiencia(int saldo, int pedido, boolean suficiente) {
            assertThat(comSaldo(saldo, 2).temEstoqueSuficiente(pedido)).isEqualTo(suficiente);
        }

        @Test
        @DisplayName("deve decrementar o saldo disponível")
        void deveDecrementar() {
            Peca peca = comSaldo(10, 2);

            peca.decrementarEstoque(4);

            assertThat(peca.getQuantidadeEstoque()).isEqualTo(6);
        }

        @Test
        @DisplayName("deve recusar decremento acima do saldo e preservar o valor")
        void deveRecusarDecrementoAcimaDoSaldo() {
            Peca peca = comSaldo(3, 2);

            assertThatThrownBy(() -> peca.decrementarEstoque(4))
                    .isInstanceOf(EstoqueInsuficienteException.class)
                    .hasMessageContaining("Pastilha de freio")
                    .hasMessageContaining("Disponível: 3")
                    .hasMessageContaining("Solicitado: 4");

            assertThat(peca.getQuantidadeEstoque()).isEqualTo(3);
        }

        @Test
        @DisplayName("deve incrementar o saldo")
        void deveIncrementar() {
            Peca peca = comSaldo(10, 2);

            peca.incrementarEstoque(5);

            assertThat(peca.getQuantidadeEstoque()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("Estoque baixo")
    class EstoqueBaixo {

        @ParameterizedTest(name = "saldo {0}, mínimo {1} -> baixo? {2}")
        @CsvSource({"5, 2, false", "2, 2, true", "1, 2, true", "0, 1, true"})
        @DisplayName("é baixo quando o saldo alcança o mínimo")
        void deveDetectarEstoqueBaixo(int saldo, int minimo, boolean baixo) {
            assertThat(comSaldo(saldo, minimo).estoqueBaixo()).isEqualTo(baixo);
        }
    }

    @Nested
    @DisplayName("Atualização parcial")
    class AtualizacaoParcial {

        @Test
        @DisplayName("deve ignorar campos nulos e textos em branco")
        void deveIgnorarNulos() {
            Peca peca = comSaldo(10, 2);

            peca.atualizarDados(null, null, null, null, null);

            assertThat(peca.getNome()).isEqualTo("Pastilha de freio");
            assertThat(peca.getPrecoVenda()).isEqualByComparingTo("120.00");
            assertThat(peca.getQuantidadeMinima()).isEqualTo(2);
            assertThat(peca.getUnidade()).isEqualTo("un");
        }

        @Test
        @DisplayName("deve aplicar apenas os campos informados")
        void deveAplicarApenasInformados() {
            Peca peca = comSaldo(10, 2);

            peca.atualizarDados("Pastilha traseira", "Nova descrição", new BigDecimal("99.90"), 5, "cx");

            assertThat(peca.getNome()).isEqualTo("Pastilha traseira");
            assertThat(peca.getDescricao()).isEqualTo("Nova descrição");
            assertThat(peca.getPrecoVenda()).isEqualByComparingTo("99.90");
            assertThat(peca.getQuantidadeMinima()).isEqualTo(5);
            assertThat(peca.getUnidade()).isEqualTo("cx");
            assertThat(peca.getQuantidadeEstoque())
                    .as("atualizar dados não mexe no saldo")
                    .isEqualTo(10);
        }

        @Test
        @DisplayName("nome em branco não deve sobrescrever o nome atual")
        void nomeEmBrancoNaoSobrescreve() {
            Peca peca = comSaldo(10, 2);

            peca.atualizarDados("   ", null, null, null, null);

            assertThat(peca.getNome()).isEqualTo("Pastilha de freio");
        }
    }

    @Test
    @DisplayName("desativar deve marcar a peça como inativa")
    void desativarMarcaInativa() {
        Peca peca = comSaldo(10, 2);

        peca.desativar();

        assertThat(peca.isAtivo()).isFalse();
    }
}
