package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.MovimentacaoEstoque;
import com.autopecas.autopecas.domain.entity.OrdemServico;
import com.autopecas.autopecas.domain.entity.Peca;
import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.exception.EstoqueInsuficienteException;
import com.autopecas.autopecas.repository.MovimentacaoEstoqueRepository;
import com.autopecas.autopecas.repository.PecaRepository;
import com.autopecas.autopecas.util.test.PecaBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstoqueService")
class EstoqueServiceTest {

    @Mock
    private PecaRepository pecaRepository;
    @Mock
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @InjectMocks
    private EstoqueService estoqueService;

    @Nested
    @DisplayName("Testes do método registrarMovimentacao()")
    class RegistrarMovimentacaoTests {

        @Test
        @DisplayName("deve reservar peça com estoque suficiente")
        void deveReservarPecaComEstoqueSuficiente() {
            // Given
            Peca peca = PecaBuilder.peca().quantidadeEstoque(10).build();
            OrdemServico ordemServico = OrdemServico.builder().numero("OS-000111").build();
            when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MovimentacaoEstoque resultado = estoqueService.registrarMovimentacao(
                    peca,
                    TipoMovimentacaoEstoque.SAIDA,
                    3,
                    "Reserva para execução",
                    ordemServico,
                    null
            );

            // Then
            assertThat(peca.getQuantidadeEstoque()).isEqualTo(7);
            assertThat(resultado.getTipo()).isEqualTo(TipoMovimentacaoEstoque.SAIDA);
            assertThat(resultado.getQuantidade()).isEqualTo(3);
            assertThat(resultado.getSaldoApos()).isEqualTo(7);
            assertThat(resultado.getOrdemServico()).isEqualTo(ordemServico);

            ArgumentCaptor<MovimentacaoEstoque> movimentacaoCaptor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
            verify(movimentacaoEstoqueRepository).save(movimentacaoCaptor.capture());
            assertThat(movimentacaoCaptor.getValue().getMotivo()).isEqualTo("Reserva para execução");
        }

        @Test
        @DisplayName("deve dar baixa de estoque ao registrar saída")
        void deveDarBaixaDeEstoqueAoRegistrarSaida() {
            // Given
            Peca peca = PecaBuilder.peca().quantidadeEstoque(5).build();
            when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MovimentacaoEstoque resultado = estoqueService.registrarMovimentacao(
                    peca,
                    TipoMovimentacaoEstoque.SAIDA,
                    2,
                    "Baixa manual",
                    null,
                    null
            );

            // Then
            assertThat(peca.getQuantidadeEstoque()).isEqualTo(3);
            assertThat(resultado.getSaldoApos()).isEqualTo(3);
        }

        @Test
        @DisplayName("deve lançar exceção específica quando o estoque é insuficiente")
        void deveLancarExcecaoEspecificaQuandoOEstoqueEInsuficiente() {
            // Given
            Peca peca = PecaBuilder.peca().nome("Filtro de óleo").quantidadeEstoque(1).build();

            // When / Then
            assertThatThrownBy(() -> estoqueService.registrarMovimentacao(
                    peca,
                    TipoMovimentacaoEstoque.SAIDA,
                    5,
                    "Reserva",
                    null,
                    null
            )).isInstanceOf(EstoqueInsuficienteException.class)
                    .hasMessage("Estoque insuficiente para 'Filtro de óleo'. Disponível: 1, Solicitado: 5");

            verify(movimentacaoEstoqueRepository, never()).save(any(MovimentacaoEstoque.class));
        }
    }
}

