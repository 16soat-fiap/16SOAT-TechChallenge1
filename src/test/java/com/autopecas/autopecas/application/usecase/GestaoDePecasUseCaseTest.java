package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.fake.RelogioFixo;
import com.autopecas.autopecas.application.fake.TransacaoDireta;
import com.autopecas.autopecas.application.port.in.GestaoDePecas;
import com.autopecas.autopecas.application.port.in.view.MovimentacaoView;
import com.autopecas.autopecas.application.port.in.view.PecaView;
import com.autopecas.autopecas.application.port.out.MovimentacaoEstoqueRepositorio;
import com.autopecas.autopecas.application.port.out.PecaRepositorio;
import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.EstoqueInsuficienteException;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.estoque.MovimentacaoEstoque;
import com.autopecas.autopecas.domain.model.estoque.Peca;
import com.autopecas.autopecas.domain.service.MovimentadorDeEstoque;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do caso de uso de peças.
 *
 * <p>O ponto mais importante aqui é a gravação explícita da peça após cada movimentação: na
 * versão anterior o saldo só persistia porque a entidade estava gerenciada pelo Hibernate.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GestaoDePecasUseCase")
class GestaoDePecasUseCaseTest {

    private static final UUID PECA_ID = UUID.randomUUID();

    @Mock
    private PecaRepositorio pecaRepositorio;
    @Mock
    private MovimentacaoEstoqueRepositorio movimentacaoRepositorio;

    private GestaoDePecas gestao;

    @BeforeEach
    void setUp() {
        gestao = new GestaoDePecasUseCase(pecaRepositorio, movimentacaoRepositorio,
                new MovimentadorDeEstoque(), new TransacaoDireta());
    }

    private static Peca pecaComSaldo(int saldo) {
        return Peca.reconstituir(PECA_ID, "PEC001", "Pastilha", "Dianteira", "Bosch",
                new BigDecimal("120.00"), saldo, 2, "un", true, RelogioFixo.INSTANTE,
                RelogioFixo.INSTANTE);
    }

    @Nested
    @DisplayName("Cadastro")
    class Cadastro {

        @Test
        @DisplayName("deve cadastrar com estoque inicial e persistir o saldo resultante")
        void deveCadastrarComEstoqueInicial() {
            when(pecaRepositorio.existePorCodigo("PEC001")).thenReturn(false);
            when(pecaRepositorio.salvar(any())).thenAnswer(chamada -> {
                Peca peca = chamada.getArgument(0);
                return peca.isNovo()
                        ? Peca.reconstituir(PECA_ID, peca.getCodigo(), peca.getNome(),
                                peca.getDescricao(), peca.getMarca(), peca.getPrecoVenda(),
                                peca.getQuantidadeEstoque(), peca.getQuantidadeMinima(),
                                peca.getUnidade(), peca.isAtivo(), RelogioFixo.INSTANTE,
                                RelogioFixo.INSTANTE)
                        : peca;
            });

            PecaView view = gestao.cadastrar(new GestaoDePecas.Cadastrar("PEC001", "Pastilha",
                    "Dianteira", new BigDecimal("120.00"), 15, 2, "un"));

            assertThat(view.quantidadeEstoque())
                    .as("o estoque inicial precisa aparecer na resposta")
                    .isEqualTo(15);

            ArgumentCaptor<Peca> salvas = ArgumentCaptor.forClass(Peca.class);
            verify(pecaRepositorio, org.mockito.Mockito.times(2)).salvar(salvas.capture());
            assertThat(salvas.getAllValues().getLast().getQuantidadeEstoque())
                    .as("a peça é gravada de novo com o saldo movimentado")
                    .isEqualTo(15);

            verify(movimentacaoRepositorio).salvar(any());
        }

        @Test
        @DisplayName("sem estoque inicial não deve gerar movimentação")
        void semEstoqueInicialNaoGeraMovimentacao() {
            when(pecaRepositorio.existePorCodigo("PEC001")).thenReturn(false);
            when(pecaRepositorio.salvar(any())).thenReturn(pecaComSaldo(0));

            gestao.cadastrar(new GestaoDePecas.Cadastrar("PEC001", "Pastilha", null,
                    new BigDecimal("120.00"), 0, 2, "un"));

            verify(movimentacaoRepositorio, never()).salvar(any());
            verify(pecaRepositorio, org.mockito.Mockito.times(1)).salvar(any());
        }

        @Test
        @DisplayName("deve aplicar os padrões de quantidade mínima e unidade")
        void deveAplicarPadroes() {
            when(pecaRepositorio.existePorCodigo("PEC001")).thenReturn(false);
            when(pecaRepositorio.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

            gestao.cadastrar(new GestaoDePecas.Cadastrar("PEC001", "Pastilha", null,
                    new BigDecimal("120.00"), null, null, null));

            ArgumentCaptor<Peca> capturado = ArgumentCaptor.forClass(Peca.class);
            verify(pecaRepositorio).salvar(capturado.capture());
            assertThat(capturado.getValue().getQuantidadeMinima()).isEqualTo(1);
            assertThat(capturado.getValue().getUnidade()).isEqualTo("un");
        }

        @Test
        @DisplayName("deve recusar código duplicado")
        void deveRecusarCodigoDuplicado() {
            when(pecaRepositorio.existePorCodigo("PEC001")).thenReturn(true);

            assertThatThrownBy(() -> gestao.cadastrar(new GestaoDePecas.Cadastrar("PEC001", "X", null,
                    BigDecimal.TEN, null, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Código da peça já está cadastrado");

            verify(pecaRepositorio, never()).salvar(any());
        }
    }

    @Nested
    @DisplayName("Movimentação manual")
    class MovimentacaoManual {

        @Test
        @DisplayName("entrada deve somar ao saldo e gravar peça e movimentação")
        void entradaSomaEGrava() {
            Peca peca = pecaComSaldo(10);
            when(pecaRepositorio.porId(PECA_ID)).thenReturn(Optional.of(peca));
            when(movimentacaoRepositorio.salvar(any())).thenAnswer(chamada -> {
                MovimentacaoEstoque m = chamada.getArgument(0);
                return MovimentacaoEstoque.reconstituir(UUID.randomUUID(), m.getPecaId(), m.getTipo(),
                        m.getQuantidade(), m.getSaldoApos(), null, m.getMotivo(),
                        m.getOrdemServicoId(), m.getExecutadoPorId(), RelogioFixo.INSTANTE);
            });

            MovimentacaoView view = gestao.registrarMovimentacao(PECA_ID,
                    new GestaoDePecas.RegistrarMovimentacao("entrada", 5, "Compra"));

            assertThat(view.tipo()).isEqualTo(TipoMovimentacaoEstoque.ENTRADA.name());
            assertThat(view.saldoApos()).isEqualTo(15);
            assertThat(view.criadoEm()).isEqualTo(RelogioFixo.INSTANTE);
            assertThat(peca.getQuantidadeEstoque()).isEqualTo(15);
            verify(pecaRepositorio).salvar(peca);
        }

        @Test
        @DisplayName("saída acima do saldo deve falhar sem gravar nada")
        void saidaAcimaDoSaldoNaoGrava() {
            when(pecaRepositorio.porId(PECA_ID)).thenReturn(Optional.of(pecaComSaldo(3)));

            assertThatThrownBy(() -> gestao.registrarMovimentacao(PECA_ID,
                    new GestaoDePecas.RegistrarMovimentacao("SAIDA", 4, "Uso")))
                    .isInstanceOf(EstoqueInsuficienteException.class);

            verify(pecaRepositorio, never()).salvar(any());
            verify(movimentacaoRepositorio, never()).salvar(any());
        }

        @Test
        @DisplayName("deve recusar tipo de movimentação desconhecido")
        void deveRecusarTipoInvalido() {
            assertThatThrownBy(() -> gestao.registrarMovimentacao(PECA_ID,
                    new GestaoDePecas.RegistrarMovimentacao("TRANSFERENCIA", 1, "x")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Tipo de movimentação inválido");
        }

        @Test
        @DisplayName("deve falhar quando a peça não existe")
        void deveFalharSemPeca() {
            when(pecaRepositorio.porId(PECA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gestao.registrarMovimentacao(PECA_ID,
                    new GestaoDePecas.RegistrarMovimentacao("ENTRADA", 1, "x")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("deve listar peças ativas por padrão")
        void deveListarAtivas() {
            when(pecaRepositorio.ativas()).thenReturn(List.of(pecaComSaldo(10)));

            List<PecaView> views = gestao.listar(false);

            assertThat(views).hasSize(1);
            assertThat(views.getFirst().estoqueBaixo()).isFalse();
            verify(pecaRepositorio, never()).comEstoqueBaixo();
        }

        @Test
        @DisplayName("deve usar a consulta de estoque baixo quando pedido")
        void deveListarEstoqueBaixo() {
            when(pecaRepositorio.comEstoqueBaixo()).thenReturn(List.of(pecaComSaldo(1)));

            List<PecaView> views = gestao.listar(true);

            assertThat(views).hasSize(1);
            assertThat(views.getFirst().estoqueBaixo()).isTrue();
            verify(pecaRepositorio, never()).ativas();
        }

        @Test
        @DisplayName("deve buscar por código")
        void deveBuscarPorCodigo() {
            when(pecaRepositorio.porCodigo("PEC001")).thenReturn(Optional.of(pecaComSaldo(10)));

            assertThat(gestao.porCodigo("PEC001").codigo()).isEqualTo("PEC001");
        }

        @Test
        @DisplayName("deve falhar ao buscar código inexistente")
        void deveFalharCodigoInexistente() {
            when(pecaRepositorio.porCodigo("XXX")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gestao.porCodigo("XXX"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    @DisplayName("desativar deve gravar a peça inativa")
    void desativarGravaInativa() {
        Peca peca = pecaComSaldo(10);
        when(pecaRepositorio.porId(PECA_ID)).thenReturn(Optional.of(peca));

        gestao.desativar(PECA_ID);

        assertThat(peca.isAtivo()).isFalse();
        verify(pecaRepositorio).salvar(peca);
    }
}
