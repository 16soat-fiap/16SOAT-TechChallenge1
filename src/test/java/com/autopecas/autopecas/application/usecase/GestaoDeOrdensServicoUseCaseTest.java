package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.fake.GeradorNumeroOSFixo;
import com.autopecas.autopecas.application.fake.RelogioFixo;
import com.autopecas.autopecas.application.fake.TransacaoDireta;
import com.autopecas.autopecas.application.pagination.Pagina;
import com.autopecas.autopecas.application.pagination.PaginaRequisicao;
import com.autopecas.autopecas.application.port.in.GestaoDeOrdensServico;
import com.autopecas.autopecas.application.port.in.view.OrdemServicoView;
import com.autopecas.autopecas.application.port.out.ClienteRepositorio;
import com.autopecas.autopecas.application.port.out.ConsultaOrdemServico;
import com.autopecas.autopecas.application.port.out.FuncionarioRepositorio;
import com.autopecas.autopecas.application.port.out.HistoricoStatusOSRepositorio;
import com.autopecas.autopecas.application.port.out.NotificadorDeStatusOS;
import com.autopecas.autopecas.application.port.out.OrdemServicoRepositorio;
import com.autopecas.autopecas.application.port.out.PecaRepositorio;
import com.autopecas.autopecas.application.port.out.ServicoRepositorio;
import com.autopecas.autopecas.application.port.out.VeiculoRepositorio;
import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.cliente.Cliente;
import com.autopecas.autopecas.domain.model.cliente.ClientePF;
import com.autopecas.autopecas.domain.model.estoque.Peca;
import com.autopecas.autopecas.domain.model.estoque.Servico;
import com.autopecas.autopecas.domain.model.funcionario.Atendente;
import com.autopecas.autopecas.domain.model.funcionario.Mecanico;
import com.autopecas.autopecas.domain.model.os.HistoricoStatusOS;
import com.autopecas.autopecas.domain.model.os.OrdemServico;
import com.autopecas.autopecas.domain.model.veiculo.Veiculo;
import com.autopecas.autopecas.domain.vo.CPF;
import com.autopecas.autopecas.domain.vo.Placa;
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

/** Testes do caso de uso de ordens de serviço, incluindo o registro do histórico de status. */
@ExtendWith(MockitoExtension.class)
@DisplayName("GestaoDeOrdensServicoUseCase")
class GestaoDeOrdensServicoUseCaseTest {

    private static final UUID OS_ID = UUID.randomUUID();
    private static final UUID CLIENTE_ID = UUID.randomUUID();
    private static final UUID VEICULO_ID = UUID.randomUUID();
    private static final UUID SERVICO_ID = UUID.randomUUID();
    private static final UUID PECA_ID = UUID.randomUUID();

    @Mock
    private OrdemServicoRepositorio ordemServicoRepositorio;
    @Mock
    private ConsultaOrdemServico consultaOrdemServico;
    @Mock
    private ClienteRepositorio clienteRepositorio;
    @Mock
    private VeiculoRepositorio veiculoRepositorio;
    @Mock
    private FuncionarioRepositorio funcionarioRepositorio;
    @Mock
    private HistoricoStatusOSRepositorio historicoRepositorio;
    @Mock
    private ServicoRepositorio servicoRepositorio;
    @Mock
    private PecaRepositorio pecaRepositorio;
    @Mock
    private NotificadorDeStatusOS notificador;

    private GestaoDeOrdensServico gestao;

    @BeforeEach
    void setUp() {
        gestao = new GestaoDeOrdensServicoUseCase(ordemServicoRepositorio, consultaOrdemServico,
                clienteRepositorio, veiculoRepositorio, funcionarioRepositorio, historicoRepositorio,
                servicoRepositorio, pecaRepositorio, notificador,
                new GeradorNumeroOSFixo(), new RelogioFixo(), new TransacaoDireta());
    }

    private static OrdemServico osNoStatus(StatusOS status) {
        return OrdemServico.reconstituir(OS_ID, "OS-000001", 1L, status, 50000, null, null, "Queixa",
                BigDecimal.ZERO, null, null, null, RelogioFixo.INSTANTE, RelogioFixo.INSTANTE,
                CLIENTE_ID, VEICULO_ID, null, null, null, List.of(), List.of());
    }

    private static OrdemServicoView view() {
        return new OrdemServicoView(OS_ID, "OS-000001", StatusOS.RECEBIDA.name(), "Queixa", null,
                BigDecimal.ZERO, RelogioFixo.INSTANTE, CLIENTE_ID, "Maria", VEICULO_ID, "ABC1D23");
    }

    private static Veiculo veiculo() {
        return Veiculo.reconstituir(VEICULO_ID, new Placa("ABC1D23"), null, null, "Fiat", "Uno",
                2020, "Prata", null, true, CLIENTE_ID);
    }

    private static Atendente atendente() {
        return Atendente.reconstituir(UUID.randomUUID(), "ATD-0001", "52998224725", "Ana",
                "ana@oficina.com", null, null, true, null);
    }

    private static Servico servico() {
        return Servico.reconstituir(SERVICO_ID, "Troca de óleo", null, new BigDecimal("150.00"),
                60, true);
    }

    private static Peca peca() {
        return Peca.reconstituir(PECA_ID, "P-001", "Filtro de óleo", null, "Bosch",
                new BigDecimal("80.00"), 10, 2, "un", true, RelogioFixo.INSTANTE,
                RelogioFixo.INSTANTE);
    }

    /** Cliente que aceita notificações — o caminho em que o aviso de status é disparado. */
    private static Cliente clienteNotificavel() {
        return ClientePF.reconstituir(CLIENTE_ID, "Maria", "maria@cliente.com", null, true, true,
                null, new CPF("52998224725"), null, null, null, null);
    }

    @Nested
    @DisplayName("Abertura")
    class Abertura {

        @Test
        @DisplayName("deve abrir a OS com número gerado e histórico de abertura pelo atendente")
        void deveAbrirComHistoricoDoAtendente() {
            Atendente ana = atendente();
            when(clienteRepositorio.existePorId(CLIENTE_ID)).thenReturn(true);
            when(veiculoRepositorio.porIdAtivo(VEICULO_ID)).thenReturn(Optional.of(veiculo()));
            when(funcionarioRepositorio.porEmail("ana@oficina.com")).thenReturn(Optional.of(ana));
            when(ordemServicoRepositorio.salvar(any())).thenReturn(osNoStatus(StatusOS.RECEBIDA));
            when(consultaOrdemServico.porId(OS_ID)).thenReturn(Optional.of(view()));

            gestao.abrir(new GestaoDeOrdensServico.Abrir(CLIENTE_ID, VEICULO_ID, "Barulho no motor",
                    null, 50000, "ana@oficina.com", null, null));

            ArgumentCaptor<OrdemServico> osSalva = ArgumentCaptor.forClass(OrdemServico.class);
            verify(ordemServicoRepositorio).salvar(osSalva.capture());
            assertThat(osSalva.getValue().getNumero()).isEqualTo("OS-000001");
            assertThat(osSalva.getValue().getStatus()).isEqualTo(StatusOS.RECEBIDA);
            assertThat(osSalva.getValue().getAtendenteRecepcaoId()).isEqualTo(ana.getId());

            ArgumentCaptor<HistoricoStatusOS> historico =
                    ArgumentCaptor.forClass(HistoricoStatusOS.class);
            verify(historicoRepositorio).salvar(historico.capture());
            assertThat(historico.getValue().getStatusAnterior()).isNull();
            assertThat(historico.getValue().getStatusNovo()).isEqualTo(StatusOS.RECEBIDA);
            assertThat(historico.getValue().getAlteradoPor()).contains("Ana", "ATD-0001", "ATENDENTE");
            assertThat(historico.getValue().getExecutadoPorId()).isEqualTo(ana.getId());
        }

        @Test
        @DisplayName("sem usuário identificado o histórico deve ser registrado como SISTEMA")
        void semUsuarioRegistraComoSistema() {
            when(clienteRepositorio.existePorId(CLIENTE_ID)).thenReturn(true);
            when(veiculoRepositorio.porIdAtivo(VEICULO_ID)).thenReturn(Optional.of(veiculo()));
            when(ordemServicoRepositorio.salvar(any())).thenReturn(osNoStatus(StatusOS.RECEBIDA));
            when(consultaOrdemServico.porId(OS_ID)).thenReturn(Optional.of(view()));

            gestao.abrir(new GestaoDeOrdensServico.Abrir(CLIENTE_ID, VEICULO_ID, "Barulho", null,
                    null, null, null, null));

            ArgumentCaptor<HistoricoStatusOS> historico =
                    ArgumentCaptor.forClass(HistoricoStatusOS.class);
            verify(historicoRepositorio).salvar(historico.capture());
            assertThat(historico.getValue().getAlteradoPor()).isEqualTo("SISTEMA");
            assertThat(historico.getValue().getExecutadoPorId()).isNull();
        }

        @Test
        @DisplayName("um funcionário que não é atendente não vira recepção da OS")
        void mecanicoNaoViraRecepcao() {
            Mecanico carlos = Mecanico.reconstituir(UUID.randomUUID(), "MEC-0001", "52998224725",
                    "Carlos", "carlos@oficina.com", null, null, true, null);
            when(clienteRepositorio.existePorId(CLIENTE_ID)).thenReturn(true);
            when(veiculoRepositorio.porIdAtivo(VEICULO_ID)).thenReturn(Optional.of(veiculo()));
            when(funcionarioRepositorio.porEmail("carlos@oficina.com")).thenReturn(Optional.of(carlos));
            when(ordemServicoRepositorio.salvar(any())).thenReturn(osNoStatus(StatusOS.RECEBIDA));
            when(consultaOrdemServico.porId(OS_ID)).thenReturn(Optional.of(view()));

            gestao.abrir(new GestaoDeOrdensServico.Abrir(CLIENTE_ID, VEICULO_ID, "Barulho", null,
                    null, "carlos@oficina.com", null, null));

            ArgumentCaptor<OrdemServico> osSalva = ArgumentCaptor.forClass(OrdemServico.class);
            verify(ordemServicoRepositorio).salvar(osSalva.capture());
            assertThat(osSalva.getValue().getAtendenteRecepcaoId()).isNull();
        }

        @Test
        @DisplayName("deve falhar quando o cliente não existe")
        void deveFalharSemCliente() {
            when(clienteRepositorio.existePorId(CLIENTE_ID)).thenReturn(false);

            assertThatThrownBy(() -> gestao.abrir(new GestaoDeOrdensServico.Abrir(CLIENTE_ID,
                    VEICULO_ID, "Barulho", null, null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Cliente não encontrado");

            verify(ordemServicoRepositorio, never()).salvar(any());
        }

        @Test
        @DisplayName("deve falhar quando o veículo está inativo ou não existe")
        void deveFalharSemVeiculoAtivo() {
            when(clienteRepositorio.existePorId(CLIENTE_ID)).thenReturn(true);
            when(veiculoRepositorio.porIdAtivo(VEICULO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gestao.abrir(new GestaoDeOrdensServico.Abrir(CLIENTE_ID,
                    VEICULO_ID, "Barulho", null, null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Veículo inativo ou não encontrado");
        }

        @Test
        @DisplayName("deve lançar os serviços e peças informados, ao preço vigente no catálogo")
        void deveAbrirComItens() {
            when(clienteRepositorio.existePorId(CLIENTE_ID)).thenReturn(true);
            when(veiculoRepositorio.porIdAtivo(VEICULO_ID)).thenReturn(Optional.of(veiculo()));
            when(servicoRepositorio.porId(SERVICO_ID)).thenReturn(Optional.of(servico()));
            when(pecaRepositorio.porId(PECA_ID)).thenReturn(Optional.of(peca()));
            when(ordemServicoRepositorio.salvar(any())).thenReturn(osNoStatus(StatusOS.RECEBIDA));
            when(consultaOrdemServico.porId(OS_ID)).thenReturn(Optional.of(view()));

            gestao.abrir(new GestaoDeOrdensServico.Abrir(CLIENTE_ID, VEICULO_ID, "Barulho", null,
                    null, null,
                    List.of(new GestaoDeOrdensServico.Abrir.ItemServico(SERVICO_ID, 2)),
                    List.of(new GestaoDeOrdensServico.Abrir.ItemPeca(PECA_ID, null))));

            ArgumentCaptor<OrdemServico> osSalva = ArgumentCaptor.forClass(OrdemServico.class);
            verify(ordemServicoRepositorio).salvar(osSalva.capture());

            assertThat(osSalva.getValue().getItensServico()).singleElement().satisfies(item -> {
                assertThat(item.getServicoId()).isEqualTo(SERVICO_ID);
                assertThat(item.getQuantidade()).isEqualTo(2);
                assertThat(item.getPrecoUnitario()).isEqualByComparingTo("150.00");
            });
            // quantidade omitida assume 1, mesma convenção do orçamento
            assertThat(osSalva.getValue().getItensPeca()).singleElement().satisfies(item -> {
                assertThat(item.getPecaId()).isEqualTo(PECA_ID);
                assertThat(item.getQuantidade()).isEqualTo(1);
                assertThat(item.getPrecoUnitario()).isEqualByComparingTo("80.00");
            });
        }

        @Test
        @DisplayName("os itens da abertura não baixam estoque — a baixa é na aprovação do orçamento")
        void itensNaAberturaNaoMovimentamEstoque() {
            when(clienteRepositorio.existePorId(CLIENTE_ID)).thenReturn(true);
            when(veiculoRepositorio.porIdAtivo(VEICULO_ID)).thenReturn(Optional.of(veiculo()));
            when(pecaRepositorio.porId(PECA_ID)).thenReturn(Optional.of(peca()));
            when(ordemServicoRepositorio.salvar(any())).thenReturn(osNoStatus(StatusOS.RECEBIDA));
            when(consultaOrdemServico.porId(OS_ID)).thenReturn(Optional.of(view()));

            gestao.abrir(new GestaoDeOrdensServico.Abrir(CLIENTE_ID, VEICULO_ID, "Barulho", null,
                    null, null, null,
                    List.of(new GestaoDeOrdensServico.Abrir.ItemPeca(PECA_ID, 3))));

            verify(pecaRepositorio, never()).salvar(any());
        }

        @Test
        @DisplayName("deve falhar quando a peça informada não existe")
        void deveFalharComPecaInexistente() {
            when(clienteRepositorio.existePorId(CLIENTE_ID)).thenReturn(true);
            when(veiculoRepositorio.porIdAtivo(VEICULO_ID)).thenReturn(Optional.of(veiculo()));
            when(pecaRepositorio.porId(PECA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gestao.abrir(new GestaoDeOrdensServico.Abrir(CLIENTE_ID,
                    VEICULO_ID, "Barulho", null, null, null, null,
                    List.of(new GestaoDeOrdensServico.Abrir.ItemPeca(PECA_ID, 1)))))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Peça não encontrada");

            verify(ordemServicoRepositorio, never()).salvar(any());
        }
    }

    @Nested
    @DisplayName("Avanço de status")
    class AvancoDeStatus {

        @Test
        @DisplayName("deve avançar e registrar o histórico com status anterior e novo")
        void deveAvancarERegistrarHistorico() {
            OrdemServico os = osNoStatus(StatusOS.RECEBIDA);
            when(ordemServicoRepositorio.porId(OS_ID)).thenReturn(Optional.of(os));
            when(ordemServicoRepositorio.salvar(any())).thenReturn(os);
            when(consultaOrdemServico.porId(OS_ID)).thenReturn(Optional.of(view()));

            gestao.avancarStatus(OS_ID, new GestaoDeOrdensServico.AvancarStatus("em_diagnostico",
                    "Veículo na baia 3", null));

            assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);

            ArgumentCaptor<HistoricoStatusOS> historico =
                    ArgumentCaptor.forClass(HistoricoStatusOS.class);
            verify(historicoRepositorio).salvar(historico.capture());
            assertThat(historico.getValue().getStatusAnterior()).isEqualTo(StatusOS.RECEBIDA);
            assertThat(historico.getValue().getStatusNovo()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
            assertThat(historico.getValue().getObservacao()).isEqualTo("Veículo na baia 3");
        }

        @Test
        @DisplayName("deve recusar status desconhecido antes de tocar no agregado")
        void deveRecusarStatusDesconhecido() {
            assertThatThrownBy(() -> gestao.avancarStatus(OS_ID,
                    new GestaoDeOrdensServico.AvancarStatus("VOANDO", null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Status inválido");

            verify(ordemServicoRepositorio, never()).porId(any());
        }

        @Test
        @DisplayName("transição inválida não deve gerar histórico")
        void transicaoInvalidaNaoGeraHistorico() {
            when(ordemServicoRepositorio.porId(OS_ID))
                    .thenReturn(Optional.of(osNoStatus(StatusOS.RECEBIDA)));

            assertThatThrownBy(() -> gestao.avancarStatus(OS_ID,
                    new GestaoDeOrdensServico.AvancarStatus("ENTREGUE", null, null)))
                    .isInstanceOf(BusinessException.class);

            verify(historicoRepositorio, never()).salvar(any());
            verify(ordemServicoRepositorio, never()).salvar(any());
        }
    }

    @Nested
    @DisplayName("Notificação de status ao cliente")
    class NotificacaoDeStatus {

        private void avancoValido() {
            OrdemServico os = osNoStatus(StatusOS.RECEBIDA);
            when(ordemServicoRepositorio.porId(OS_ID)).thenReturn(Optional.of(os));
            when(ordemServicoRepositorio.salvar(any())).thenReturn(os);
            when(consultaOrdemServico.porId(OS_ID)).thenReturn(Optional.of(view()));
        }

        @Test
        @DisplayName("deve notificar o cliente com o status anterior e o novo")
        void deveNotificar() {
            avancoValido();
            when(clienteRepositorio.porId(CLIENTE_ID)).thenReturn(Optional.of(clienteNotificavel()));

            gestao.avancarStatus(OS_ID, new GestaoDeOrdensServico.AvancarStatus("EM_DIAGNOSTICO",
                    "Veículo na baia 3", null));

            ArgumentCaptor<NotificadorDeStatusOS.MudancaDeStatus> aviso =
                    ArgumentCaptor.forClass(NotificadorDeStatusOS.MudancaDeStatus.class);
            verify(notificador).notificarMudancaDeStatus(aviso.capture());
            assertThat(aviso.getValue().numeroOS()).isEqualTo("OS-000001");
            assertThat(aviso.getValue().statusAnterior()).isEqualTo("RECEBIDA");
            assertThat(aviso.getValue().emailCliente()).isEqualTo("maria@cliente.com");
            assertThat(aviso.getValue().observacao()).isEqualTo("Veículo na baia 3");
        }

        @Test
        @DisplayName("não deve notificar quem recusou notificações")
        void naoNotificaQuemRecusou() {
            avancoValido();
            Cliente semAviso = ClientePF.reconstituir(CLIENTE_ID, "Maria", "maria@cliente.com", null,
                    false, true, null, new CPF("52998224725"), null, null, null, null);
            when(clienteRepositorio.porId(CLIENTE_ID)).thenReturn(Optional.of(semAviso));

            gestao.avancarStatus(OS_ID,
                    new GestaoDeOrdensServico.AvancarStatus("EM_DIAGNOSTICO", null, null));

            verify(notificador, never()).notificarMudancaDeStatus(any());
        }

        @Test
        @DisplayName("não deve notificar cliente sem e-mail cadastrado")
        void naoNotificaSemEmail() {
            avancoValido();
            Cliente semEmail = ClientePF.reconstituir(CLIENTE_ID, "Maria", null, null, true, true,
                    null, new CPF("52998224725"), null, null, null, null);
            when(clienteRepositorio.porId(CLIENTE_ID)).thenReturn(Optional.of(semEmail));

            gestao.avancarStatus(OS_ID,
                    new GestaoDeOrdensServico.AvancarStatus("EM_DIAGNOSTICO", null, null));

            verify(notificador, never()).notificarMudancaDeStatus(any());
        }

        @Test
        @DisplayName("falha ao notificar não desfaz o avanço de status")
        void falhaAoNotificarNaoQuebraOAvanco() {
            OrdemServico os = osNoStatus(StatusOS.RECEBIDA);
            when(ordemServicoRepositorio.porId(OS_ID)).thenReturn(Optional.of(os));
            when(ordemServicoRepositorio.salvar(any())).thenReturn(os);
            when(consultaOrdemServico.porId(OS_ID)).thenReturn(Optional.of(view()));
            when(clienteRepositorio.porId(CLIENTE_ID))
                    .thenThrow(new IllegalStateException("banco fora do ar"));

            OrdemServicoView resultado = gestao.avancarStatus(OS_ID,
                    new GestaoDeOrdensServico.AvancarStatus("EM_DIAGNOSTICO", null, null));

            assertThat(resultado).isNotNull();
            assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
            verify(ordemServicoRepositorio).salvar(any());
        }
    }

    @Nested
    @DisplayName("Diagnóstico e mecânico")
    class DiagnosticoEMecanico {

        @Test
        @DisplayName("deve registrar diagnóstico quando a OS está em diagnóstico")
        void deveRegistrarDiagnostico() {
            OrdemServico os = osNoStatus(StatusOS.EM_DIAGNOSTICO);
            when(ordemServicoRepositorio.porId(OS_ID)).thenReturn(Optional.of(os));
            when(ordemServicoRepositorio.salvar(any())).thenReturn(os);
            when(consultaOrdemServico.porId(OS_ID)).thenReturn(Optional.of(view()));

            gestao.registrarDiagnostico(OS_ID, "Rolamento com folga");

            assertThat(os.getDiagnostico()).isEqualTo("Rolamento com folga");
        }

        @Test
        @DisplayName("deve recusar diagnóstico fora do status EM_DIAGNOSTICO")
        void deveRecusarDiagnosticoForaDoStatus() {
            when(ordemServicoRepositorio.porId(OS_ID))
                    .thenReturn(Optional.of(osNoStatus(StatusOS.RECEBIDA)));

            assertThatThrownBy(() -> gestao.registrarDiagnostico(OS_ID, "qualquer"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("EM_DIAGNOSTICO");
        }

        @Test
        @DisplayName("deve atribuir mecânico existente")
        void deveAtribuirMecanico() {
            OrdemServico os = osNoStatus(StatusOS.EM_DIAGNOSTICO);
            Mecanico carlos = Mecanico.reconstituir(UUID.randomUUID(), "MEC-0001", "52998224725",
                    "Carlos", null, null, null, true, null);
            when(ordemServicoRepositorio.porId(OS_ID)).thenReturn(Optional.of(os));
            when(funcionarioRepositorio.porId(carlos.getId())).thenReturn(Optional.of(carlos));
            when(ordemServicoRepositorio.salvar(any())).thenReturn(os);
            when(consultaOrdemServico.porId(OS_ID)).thenReturn(Optional.of(view()));

            gestao.atribuirMecanico(OS_ID, carlos.getId());

            assertThat(os.getMecanicoResponsavelId()).isEqualTo(carlos.getId());
        }

        @Test
        @DisplayName("deve recusar atribuir um funcionário que não é mecânico")
        void deveRecusarAtendenteComoMecanico() {
            Atendente ana = atendente();
            when(ordemServicoRepositorio.porId(OS_ID))
                    .thenReturn(Optional.of(osNoStatus(StatusOS.EM_DIAGNOSTICO)));
            when(funcionarioRepositorio.porId(ana.getId())).thenReturn(Optional.of(ana));

            assertThatThrownBy(() -> gestao.atribuirMecanico(OS_ID, ana.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("não é um mecânico");

            verify(ordemServicoRepositorio, never()).salvar(any());
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("deve repassar os filtros para a query port")
        void deveRepassarFiltros() {
            PaginaRequisicao paginacao = PaginaRequisicao.de(0, 20);
            when(consultaOrdemServico.listar(any(), any()))
                    .thenReturn(new Pagina<>(List.of(view()), 0, 20, 1L));

            Pagina<OrdemServicoView> pagina = gestao.listar(StatusOS.RECEBIDA, null, null, paginacao);

            assertThat(pagina.conteudo()).hasSize(1);
            assertThat(pagina.totalElementos()).isEqualTo(1L);

            ArgumentCaptor<ConsultaOrdemServico.Filtro> filtro =
                    ArgumentCaptor.forClass(ConsultaOrdemServico.Filtro.class);
            verify(consultaOrdemServico).listar(filtro.capture(), any());
            assertThat(filtro.getValue().status()).isEqualTo(StatusOS.RECEBIDA);
            assertThat(filtro.getValue().clienteId()).isNull();
        }

        @Test
        @DisplayName("deve falhar ao buscar número inexistente")
        void deveFalharNumeroInexistente() {
            when(consultaOrdemServico.porNumero("OS-999999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gestao.porNumero("OS-999999"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("OS-999999");
        }
    }
}
