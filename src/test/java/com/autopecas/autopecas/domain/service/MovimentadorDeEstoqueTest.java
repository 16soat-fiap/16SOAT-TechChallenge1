package com.autopecas.autopecas.domain.service;

import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.domain.exception.EstoqueInsuficienteException;
import com.autopecas.autopecas.domain.model.estoque.MovimentacaoEstoque;
import com.autopecas.autopecas.domain.model.estoque.Peca;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes do domain service que substituiu o antigo EstoqueService.
 *
 * <p>A diferença que importa: aqui não há repositório nenhum. O serviço aplica o efeito na peça
 * e devolve o registro a ser gravado, então a regra "todo movimento de saldo gera movimentação"
 * é testável sem infraestrutura.
 */
@DisplayName("MovimentadorDeEstoque")
class MovimentadorDeEstoqueTest {

    private final MovimentadorDeEstoque movimentador = new MovimentadorDeEstoque();

    private static Peca pecaComSaldo(int saldo) {
        return Peca.reconstituir(UUID.randomUUID(), "PEC001", "Pastilha", null, null,
                new BigDecimal("120.00"), saldo, 2, "un", true,
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    @DisplayName("entrada deve somar ao saldo e registrar o saldo resultante")
    void entradaSomaAoSaldo() {
        Peca peca = pecaComSaldo(10);

        MovimentacaoEstoque movimentacao = movimentador.movimentar(peca,
                TipoMovimentacaoEstoque.ENTRADA, 5, "Compra", null, null);

        assertThat(peca.getQuantidadeEstoque()).isEqualTo(15);
        assertThat(movimentacao.getTipo()).isEqualTo(TipoMovimentacaoEstoque.ENTRADA);
        assertThat(movimentacao.getQuantidade()).isEqualTo(5);
        assertThat(movimentacao.getSaldoApos()).isEqualTo(15);
        assertThat(movimentacao.getMotivo()).isEqualTo("Compra");
        assertThat(movimentacao.getPecaId()).isEqualTo(peca.getId());
    }

    @Test
    @DisplayName("saída deve subtrair do saldo e registrar o saldo resultante")
    void saidaSubtraiDoSaldo() {
        Peca peca = pecaComSaldo(10);
        UUID osId = UUID.randomUUID();

        MovimentacaoEstoque movimentacao = movimentador.movimentar(peca,
                TipoMovimentacaoEstoque.SAIDA, 4, "Uso na OS", osId, null);

        assertThat(peca.getQuantidadeEstoque()).isEqualTo(6);
        assertThat(movimentacao.getSaldoApos()).isEqualTo(6);
        assertThat(movimentacao.getOrdemServicoId()).isEqualTo(osId);
    }

    @Test
    @DisplayName("saída acima do saldo deve falhar sem alterar a peça")
    void saidaAcimaDoSaldoFalhaSemEfeito() {
        Peca peca = pecaComSaldo(3);

        assertThatThrownBy(() -> movimentador.movimentar(peca, TipoMovimentacaoEstoque.SAIDA, 4,
                "Uso na OS", null, null))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .hasMessageContaining("Disponível: 3");

        assertThat(peca.getQuantidadeEstoque())
                .as("nenhum efeito colateral parcial")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("saída exata do saldo é permitida e zera o estoque")
    void saidaExataZeraEstoque() {
        Peca peca = pecaComSaldo(4);

        MovimentacaoEstoque movimentacao = movimentador.movimentar(peca,
                TipoMovimentacaoEstoque.SAIDA, 4, "Uso na OS", null, null);

        assertThat(peca.getQuantidadeEstoque()).isZero();
        assertThat(movimentacao.getSaldoApos()).isZero();
    }

    @Test
    @DisplayName("a movimentação devolvida ainda não foi persistida")
    void movimentacaoDevolvidaEhNova() {
        Peca peca = pecaComSaldo(10);

        MovimentacaoEstoque movimentacao = movimentador.movimentar(peca,
                TipoMovimentacaoEstoque.ENTRADA, 1, "Ajuste", null, null);

        assertThat(movimentacao.getId()).isNull();
        assertThat(movimentacao.getCriadoEm()).isNull();
    }
}
