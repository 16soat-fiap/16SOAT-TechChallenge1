package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.fake.RelogioFixo;
import com.autopecas.autopecas.application.fake.TransacaoDireta;
import com.autopecas.autopecas.application.port.in.GestaoDeOrcamentos;
import com.autopecas.autopecas.application.port.in.view.OrcamentoView;
import com.autopecas.autopecas.application.port.out.MovimentacaoEstoqueRepositorio;
import com.autopecas.autopecas.application.port.out.OrcamentoRepositorio;
import com.autopecas.autopecas.application.port.out.OrdemServicoRepositorio;
import com.autopecas.autopecas.application.port.out.PecaRepositorio;
import com.autopecas.autopecas.application.port.out.ServicoRepositorio;
import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.domain.enums.StatusOrcamento;
import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.EstoqueInsuficienteException;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.estoque.MovimentacaoEstoque;
import com.autopecas.autopecas.domain.model.estoque.Peca;
import com.autopecas.autopecas.domain.model.estoque.Servico;
import com.autopecas.autopecas.domain.model.orcamento.ItemOrcamentoPeca;
import com.autopecas.autopecas.domain.model.orcamento.ItemOrcamentoServico;
import com.autopecas.autopecas.domain.model.orcamento.Orcamento;
import com.autopecas.autopecas.domain.model.os.OrdemServico;
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
import java.time.LocalDateTime;
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
 * Testes do caso de uso de orçamentos, com atenção especial à aprovação — a operação de maior
 * alcance do sistema.
 *
 * <p>Os dublês são das ports do próprio projeto, não de interfaces do Spring Data: é isso que
 * a inversão de dependência compra. Transação e relógio são fakes de verdade, porque precisam
 * ter comportamento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GestaoDeOrcamentosUseCase")
class GestaoDeOrcamentosUseCaseTest {

    private static final UUID OS_ID = UUID.randomUUID();
    private static final UUID ORCAMENTO_ID = UUID.randomUUID();
    private static final UUID PECA_ID = UUID.randomUUID();
    private static final UUID SERVICO_ID = UUID.randomUUID();

    @Mock
    private OrcamentoRepositorio orcamentoRepositorio;
    @Mock
    private OrdemServicoRepositorio ordemServicoRepositorio;
    @Mock
    private ServicoRepositorio servicoRepositorio;
    @Mock
    private PecaRepositorio pecaRepositorio;
    @Mock
    private MovimentacaoEstoqueRepositorio movimentacaoRepositorio;

    private GestaoDeOrcamentos gestao;

    @BeforeEach
    void setUp() {
        gestao = new GestaoDeOrcamentosUseCase(orcamentoRepositorio, ordemServicoRepositorio,
                servicoRepositorio, pecaRepositorio, movimentacaoRepositorio,
                new MovimentadorDeEstoque(), new RelogioFixo(), new TransacaoDireta());
    }

    private static Orcamento orcamento(StatusOrcamento status, List<ItemOrcamentoPeca> itensPeca) {
        return Orcamento.reconstituir(ORCAMENTO_ID, 1, status, 0L, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("500.00"), null, null, null, null, null, null, null,
                RelogioFixo.INSTANTE, RelogioFixo.INSTANTE, OS_ID, null,
                List.of(ItemOrcamentoServico.criar(SERVICO_ID, 1, new BigDecimal("200.00"))), itensPeca);
    }

    private static OrdemServico osAguardandoAprovacao() {
        return OrdemServico.reconstituir(OS_ID, "OS-000001", 1L, StatusOS.AGUARDANDO_APROVACAO, 50000,
                null, null, "Queixa", BigDecimal.ZERO, null, null, null, RelogioFixo.INSTANTE,
                RelogioFixo.INSTANTE, UUID.randomUUID(), UUID.randomUUID(), null, null, null,
                List.of(), List.of());
    }

    private static Peca pecaComSaldo(int saldo) {
        return Peca.reconstituir(PECA_ID, "PEC001", "Pastilha", null, null, new BigDecimal("100.00"),
                saldo, 2, "un", true, RelogioFixo.INSTANTE, RelogioFixo.INSTANTE);
    }

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("deve montar o orçamento copiando os preços do catálogo e recalcular o total")
        void deveCopiarPrecosDoCatalogo() {
            Servico servico = Servico.reconstituir(SERVICO_ID, "Alinhamento", null,
                    new BigDecimal("180.00"), 60, true);
            when(ordemServicoRepositorio.existePorId(OS_ID)).thenReturn(true);
            when(orcamentoRepositorio.existeAprovadoParaOrdemServico(OS_ID)).thenReturn(false);
            when(orcamentoRepositorio.proximaVersao(OS_ID)).thenReturn(3);
            when(servicoRepositorio.porId(SERVICO_ID)).thenReturn(Optional.of(servico));
            when(pecaRepositorio.porId(PECA_ID)).thenReturn(Optional.of(pecaComSaldo(10)));
            when(orcamentoRepositorio.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

            gestao.criar(OS_ID, new GestaoDeOrcamentos.Criar(
                    List.of(new GestaoDeOrcamentos.Criar.ItemServico(SERVICO_ID, 2)),
                    List.of(new GestaoDeOrcamentos.Criar.ItemPeca(PECA_ID, 3)),
                    "À vista", 5, null, null));

            ArgumentCaptor<Orcamento> capturado = ArgumentCaptor.forClass(Orcamento.class);
            verify(orcamentoRepositorio).salvar(capturado.capture());
            Orcamento salvo = capturado.getValue();

            assertThat(salvo.getVersao()).isEqualTo(3);
            assertThat(salvo.getStatus()).isEqualTo(StatusOrcamento.RASCUNHO);
            assertThat(salvo.getValorMaoObra()).isEqualByComparingTo("360.00");
            assertThat(salvo.getValorPecas()).isEqualByComparingTo("300.00");
            assertThat(salvo.getValorTotal()).isEqualByComparingTo("660.00");
        }

        @Test
        @DisplayName("deve assumir quantidade 1 quando o item não informa quantidade")
        void deveAssumirQuantidadeUm() {
            when(ordemServicoRepositorio.existePorId(OS_ID)).thenReturn(true);
            when(orcamentoRepositorio.proximaVersao(OS_ID)).thenReturn(1);
            when(pecaRepositorio.porId(PECA_ID)).thenReturn(Optional.of(pecaComSaldo(10)));
            when(orcamentoRepositorio.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

            gestao.criar(OS_ID, new GestaoDeOrcamentos.Criar(null,
                    List.of(new GestaoDeOrcamentos.Criar.ItemPeca(PECA_ID, null)),
                    null, null, null, null));

            ArgumentCaptor<Orcamento> capturado = ArgumentCaptor.forClass(Orcamento.class);
            verify(orcamentoRepositorio).salvar(capturado.capture());
            assertThat(capturado.getValue().getItensPeca().getFirst().getQuantidade()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve recusar novo orçamento quando já existe um aprovado")
        void deveRecusarComOrcamentoAprovado() {
            when(ordemServicoRepositorio.existePorId(OS_ID)).thenReturn(true);
            when(orcamentoRepositorio.existeAprovadoParaOrdemServico(OS_ID)).thenReturn(true);

            assertThatThrownBy(() -> gestao.criar(OS_ID,
                    new GestaoDeOrcamentos.Criar(null, null, null, null, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Já existe um orçamento aprovado");

            verify(orcamentoRepositorio, never()).salvar(any());
        }

        @Test
        @DisplayName("deve falhar quando a OS não existe")
        void deveFalharSemOs() {
            when(ordemServicoRepositorio.existePorId(OS_ID)).thenReturn(false);

            assertThatThrownBy(() -> gestao.criar(OS_ID,
                    new GestaoDeOrcamentos.Criar(null, null, null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Aprovação")
    class Aprovacao {

        @Test
        @DisplayName("deve aprovar, avançar a OS, copiar os itens e baixar o estoque")
        void deveAprovarEBaixarEstoque() {
            Orcamento orcamento = orcamento(StatusOrcamento.ENVIADA,
                    List.of(ItemOrcamentoPeca.criar(PECA_ID, 3, new BigDecimal("100.00"))));
            OrdemServico os = osAguardandoAprovacao();
            Peca peca = pecaComSaldo(10);

            when(orcamentoRepositorio.porId(ORCAMENTO_ID)).thenReturn(Optional.of(orcamento));
            when(orcamentoRepositorio.existeAprovadoParaOrdemServico(OS_ID)).thenReturn(false);
            when(ordemServicoRepositorio.porId(OS_ID)).thenReturn(Optional.of(os));
            when(pecaRepositorio.porId(PECA_ID)).thenReturn(Optional.of(peca));

            OrcamentoView view = gestao.aprovar(OS_ID, ORCAMENTO_ID);

            assertThat(view.status()).isEqualTo(StatusOrcamento.APROVADA.name());
            assertThat(orcamento.getDataRespostaCliente()).isEqualTo(RelogioFixo.INSTANTE);

            assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
            assertThat(os.getValorTotalAprovado()).isEqualByComparingTo("500.00");
            assertThat(os.getDataInicioExecucao()).isEqualTo(RelogioFixo.INSTANTE);
            assertThat(os.getItensServico()).hasSize(1);
            assertThat(os.getItensPeca()).hasSize(1);

            assertThat(peca.getQuantidadeEstoque())
                    .as("3 unidades saíram do estoque de 10")
                    .isEqualTo(7);
        }

        @Test
        @DisplayName("deve gravar explicitamente a peça alterada, sem depender de dirty checking")
        void deveGravarPecaAlterada() {
            Orcamento orcamento = orcamento(StatusOrcamento.ENVIADA,
                    List.of(ItemOrcamentoPeca.criar(PECA_ID, 4, new BigDecimal("100.00"))));
            when(orcamentoRepositorio.porId(ORCAMENTO_ID)).thenReturn(Optional.of(orcamento));
            when(ordemServicoRepositorio.porId(OS_ID)).thenReturn(Optional.of(osAguardandoAprovacao()));
            when(pecaRepositorio.porId(PECA_ID)).thenReturn(Optional.of(pecaComSaldo(10)));

            gestao.aprovar(OS_ID, ORCAMENTO_ID);

            ArgumentCaptor<Peca> pecaSalva = ArgumentCaptor.forClass(Peca.class);
            verify(pecaRepositorio).salvar(pecaSalva.capture());
            assertThat(pecaSalva.getValue().getQuantidadeEstoque()).isEqualTo(6);
        }

        @Test
        @DisplayName("deve registrar a movimentação de saída referenciando a OS")
        void deveRegistrarMovimentacaoDeSaida() {
            Orcamento orcamento = orcamento(StatusOrcamento.ENVIADA,
                    List.of(ItemOrcamentoPeca.criar(PECA_ID, 2, new BigDecimal("100.00"))));
            when(orcamentoRepositorio.porId(ORCAMENTO_ID)).thenReturn(Optional.of(orcamento));
            when(ordemServicoRepositorio.porId(OS_ID)).thenReturn(Optional.of(osAguardandoAprovacao()));
            when(pecaRepositorio.porId(PECA_ID)).thenReturn(Optional.of(pecaComSaldo(10)));

            gestao.aprovar(OS_ID, ORCAMENTO_ID);

            ArgumentCaptor<MovimentacaoEstoque> capturado =
                    ArgumentCaptor.forClass(MovimentacaoEstoque.class);
            verify(movimentacaoRepositorio).salvar(capturado.capture());
            MovimentacaoEstoque movimentacao = capturado.getValue();

            assertThat(movimentacao.getTipo()).isEqualTo(TipoMovimentacaoEstoque.SAIDA);
            assertThat(movimentacao.getQuantidade()).isEqualTo(2);
            assertThat(movimentacao.getSaldoApos()).isEqualTo(8);
            assertThat(movimentacao.getOrdemServicoId()).isEqualTo(OS_ID);
            assertThat(movimentacao.getMotivo()).contains("OS-000001");
        }

        @Test
        @DisplayName("deve falhar sem gravar nada quando o estoque é insuficiente")
        void deveFalharComEstoqueInsuficiente() {
            Orcamento orcamento = orcamento(StatusOrcamento.ENVIADA,
                    List.of(ItemOrcamentoPeca.criar(PECA_ID, 20, new BigDecimal("100.00"))));
            when(orcamentoRepositorio.porId(ORCAMENTO_ID)).thenReturn(Optional.of(orcamento));
            when(ordemServicoRepositorio.porId(OS_ID)).thenReturn(Optional.of(osAguardandoAprovacao()));
            when(pecaRepositorio.porId(PECA_ID)).thenReturn(Optional.of(pecaComSaldo(10)));

            assertThatThrownBy(() -> gestao.aprovar(OS_ID, ORCAMENTO_ID))
                    .isInstanceOf(EstoqueInsuficienteException.class);

            verify(pecaRepositorio, never()).salvar(any());
            verify(movimentacaoRepositorio, never()).salvar(any());
            verify(orcamentoRepositorio, never()).salvar(any());
        }

        @Test
        @DisplayName("deve recusar aprovação quando a OS não está aguardando aprovação")
        void deveRecusarQuandoOsNaoPodeAvancar() {
            Orcamento orcamento = orcamento(StatusOrcamento.ENVIADA, List.of());
            OrdemServico os = OrdemServico.reconstituir(OS_ID, "OS-000001", 1L, StatusOS.RECEBIDA,
                    null, null, null, "q", BigDecimal.ZERO, null, null, null, RelogioFixo.INSTANTE,
                    RelogioFixo.INSTANTE, UUID.randomUUID(), UUID.randomUUID(), null, null, null,
                    List.of(), List.of());
            when(orcamentoRepositorio.porId(ORCAMENTO_ID)).thenReturn(Optional.of(orcamento));
            when(ordemServicoRepositorio.porId(OS_ID)).thenReturn(Optional.of(os));

            assertThatThrownBy(() -> gestao.aprovar(OS_ID, ORCAMENTO_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Transição de status inválida");

            verify(ordemServicoRepositorio, never()).salvar(any());
        }

        @Test
        @DisplayName("deve recusar quando o orçamento não pertence à OS informada")
        void deveRecusarOrcamentoDeOutraOs() {
            when(orcamentoRepositorio.porId(ORCAMENTO_ID))
                    .thenReturn(Optional.of(orcamento(StatusOrcamento.ENVIADA, List.of())));

            assertThatThrownBy(() -> gestao.aprovar(UUID.randomUUID(), ORCAMENTO_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("não pertence à OS");
        }
    }

    @Nested
    @DisplayName("Envio e rejeição")
    class EnvioERejeicao {

        @Test
        @DisplayName("deve enviar registrando a data do relógio")
        void deveEnviar() {
            Orcamento orcamento = orcamento(StatusOrcamento.RASCUNHO, List.of());
            when(orcamentoRepositorio.porId(ORCAMENTO_ID)).thenReturn(Optional.of(orcamento));
            when(orcamentoRepositorio.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

            OrcamentoView view = gestao.enviar(OS_ID, ORCAMENTO_ID);

            assertThat(view.status()).isEqualTo(StatusOrcamento.ENVIADA.name());
            assertThat(orcamento.getDataEnvio()).isEqualTo(RelogioFixo.INSTANTE);
        }

        @Test
        @DisplayName("deve rejeitar guardando o motivo")
        void deveRejeitar() {
            Orcamento orcamento = orcamento(StatusOrcamento.ENVIADA, List.of());
            when(orcamentoRepositorio.porId(ORCAMENTO_ID)).thenReturn(Optional.of(orcamento));
            when(orcamentoRepositorio.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

            gestao.rejeitar(OS_ID, ORCAMENTO_ID, "Valor acima do esperado");

            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.CANCELADA);
            assertThat(orcamento.getMotivoRejeicao()).isEqualTo("Valor acima do esperado");
        }

        @Test
        @DisplayName("deve falhar quando o orçamento não existe")
        void deveFalharSemOrcamento() {
            when(orcamentoRepositorio.porId(ORCAMENTO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gestao.enviar(OS_ID, ORCAMENTO_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Orcamento não encontrado");
        }
    }

    @Nested
    @DisplayName("Listagem")
    class Listagem {

        @Test
        @DisplayName("deve listar os orçamentos da OS")
        void deveListar() {
            when(ordemServicoRepositorio.existePorId(OS_ID)).thenReturn(true);
            when(orcamentoRepositorio.daOrdemServico(OS_ID))
                    .thenReturn(List.of(orcamento(StatusOrcamento.ENVIADA, List.of())));

            List<OrcamentoView> views = gestao.daOrdemServico(OS_ID);

            assertThat(views).hasSize(1);
            assertThat(views.getFirst().versao()).isEqualTo(1);
            assertThat(views.getFirst().criadoEm()).isEqualTo(RelogioFixo.INSTANTE);
        }

        @Test
        @DisplayName("deve falhar quando a OS não existe")
        void deveFalharSemOs() {
            when(ordemServicoRepositorio.existePorId(OS_ID)).thenReturn(false);

            assertThatThrownBy(() -> gestao.daOrdemServico(OS_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
