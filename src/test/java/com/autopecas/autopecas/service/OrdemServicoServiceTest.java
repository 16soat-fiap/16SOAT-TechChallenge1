package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Atendente;
import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.domain.entity.Funcionario;
import com.autopecas.autopecas.domain.entity.HistoricoStatusOS;
import com.autopecas.autopecas.domain.entity.Mecanico;
import com.autopecas.autopecas.domain.entity.OrdemServico;
import com.autopecas.autopecas.domain.entity.Veiculo;
import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.dto.os.AtribuirMecanicoDTO;
import com.autopecas.autopecas.dto.os.AvancarStatusDTO;
import com.autopecas.autopecas.dto.os.DiagnosticoDTO;
import com.autopecas.autopecas.dto.os.OrdemServicoCreateDTO;
import com.autopecas.autopecas.dto.os.OrdemServicoResponseDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.OrdemServicoMapper;
import com.autopecas.autopecas.repository.ClienteRepository;
import com.autopecas.autopecas.repository.FuncionarioRepository;
import com.autopecas.autopecas.repository.HistoricoStatusOsRepository;
import com.autopecas.autopecas.repository.OrdemServicoRepository;
import com.autopecas.autopecas.repository.VeiculoRepository;
import com.autopecas.autopecas.util.test.ClienteBuilder;
import com.autopecas.autopecas.util.test.FuncionarioBuilder;
import com.autopecas.autopecas.util.test.VeiculoBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrdemServicoService")
class OrdemServicoServiceTest {

    @Mock
    private OrdemServicoRepository ordemServicoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private VeiculoRepository veiculoRepository;
    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private HistoricoStatusOsRepository historicoStatusOsRepository;
    @Mock
    private OrdemServicoMapper ordemServicoMapper;

    @InjectMocks
    private OrdemServicoService ordemServicoService;

    private Cliente cliente;
    private Veiculo veiculo;
    private Atendente atendente;
    private Mecanico mecanico;

    @BeforeEach
    void setUp() {
        cliente = ClienteBuilder.clientePF().build();
        veiculo = VeiculoBuilder.veiculo(cliente).build();
        atendente = FuncionarioBuilder.atendente().build();
        mecanico = FuncionarioBuilder.mecanico().build();
    }

    @Nested
    @DisplayName("Testes do método criar()")
    class CriarTests {

        @Test
        @DisplayName("deve criar ordem de serviço e persistir histórico de abertura quando atendente é encontrado")
        void deveCriarOrdemServicoEPersistirHistoricoDeAberturaQuandoAtendenteEncontrado() {
            // Given
            OrdemServicoCreateDTO dto = new OrdemServicoCreateDTO(
                    cliente.getId(),
                    veiculo.getId(),
                    "Barulho ao frear",
                    "Cliente relatou vibração",
                    65432
            );
            OrdemServicoResponseDTO response = responseDTO(StatusOS.RECEBIDA, "OS-000001");

            when(clienteRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
            when(veiculoRepository.findByIdAndAtivoTrue(veiculo.getId())).thenReturn(Optional.of(veiculo));
            when(ordemServicoRepository.proximoNumero()).thenReturn(1L);
            when(funcionarioRepository.findByEmail(atendente.getEmail())).thenReturn(Optional.of(atendente));
            when(ordemServicoRepository.save(any(OrdemServico.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(ordemServicoMapper.toResponse(any(OrdemServico.class))).thenReturn(response);

            // When
            OrdemServicoResponseDTO resultado = ordemServicoService.criar(dto, atendente.getEmail());

            // Then
            assertThat(resultado).isEqualTo(response);

            ArgumentCaptor<OrdemServico> ordemServicoCaptor = ArgumentCaptor.forClass(OrdemServico.class);
            verify(ordemServicoRepository).save(ordemServicoCaptor.capture());
            OrdemServico ordemSalva = ordemServicoCaptor.getValue();
            assertThat(ordemSalva.getNumero()).isEqualTo("OS-000001");
            assertThat(ordemSalva.getStatus()).isEqualTo(StatusOS.RECEBIDA);
            assertThat(ordemSalva.getCliente()).isEqualTo(cliente);
            assertThat(ordemSalva.getVeiculo()).isEqualTo(veiculo);
            assertThat(ordemSalva.getAtendenteRecepcao()).isEqualTo(atendente);
            assertThat(ordemSalva.getQueixaCliente()).isEqualTo("Barulho ao frear");
            assertThat(ordemSalva.getObservacoesEntrada()).isEqualTo("Cliente relatou vibração");
            assertThat(ordemSalva.getQuilometragemEntrada()).isEqualTo(65432);
            assertThat(ordemSalva.getValorTotalAprovado()).isEqualByComparingTo(BigDecimal.ZERO);

            ArgumentCaptor<HistoricoStatusOS> historicoCaptor = ArgumentCaptor.forClass(HistoricoStatusOS.class);
            verify(historicoStatusOsRepository).save(historicoCaptor.capture());
            HistoricoStatusOS historico = historicoCaptor.getValue();
            assertThat(historico.getStatusAnterior()).isNull();
            assertThat(historico.getStatusNovo()).isEqualTo(StatusOS.RECEBIDA);
            assertThat(historico.getObservacao()).isEqualTo("Ordem de serviço aberta.");
            assertThat(historico.getExecutadoPor()).isEqualTo(atendente);
            assertThat(historico.getAlteradoPor()).isEqualTo(atendente.getIdentificacao() + " — " + atendente.getTipo());
        }

        @Test
        @DisplayName("deve criar ordem de serviço com histórico do sistema quando email do atendente não é informado")
        void deveCriarOrdemServicoComHistoricoDoSistemaQuandoEmailDoAtendenteNaoInformado() {
            // Given
            OrdemServicoCreateDTO dto = new OrdemServicoCreateDTO(
                    cliente.getId(),
                    veiculo.getId(),
                    "Motor falhando",
                    null,
                    45000
            );
            OrdemServicoResponseDTO response = responseDTO(StatusOS.RECEBIDA, "OS-000002");

            when(clienteRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
            when(veiculoRepository.findByIdAndAtivoTrue(veiculo.getId())).thenReturn(Optional.of(veiculo));
            when(ordemServicoRepository.proximoNumero()).thenReturn(2L);
            when(ordemServicoRepository.save(any(OrdemServico.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(ordemServicoMapper.toResponse(any(OrdemServico.class))).thenReturn(response);

            // When
            OrdemServicoResponseDTO resultado = ordemServicoService.criar(dto, null);

            // Then
            assertThat(resultado).isEqualTo(response);

            ArgumentCaptor<HistoricoStatusOS> historicoCaptor = ArgumentCaptor.forClass(HistoricoStatusOS.class);
            verify(historicoStatusOsRepository).save(historicoCaptor.capture());
            HistoricoStatusOS historico = historicoCaptor.getValue();
            assertThat(historico.getExecutadoPor()).isNull();
            assertThat(historico.getAlteradoPor()).isEqualTo("SISTEMA");
            assertThat(historico.getStatusNovo()).isEqualTo(StatusOS.RECEBIDA);
        }

        @Test
        @DisplayName("deve lançar exceção quando cliente não é encontrado")
        void deveLancarExcecaoQuandoClienteNaoEncontrado() {
            // Given
            OrdemServicoCreateDTO dto = new OrdemServicoCreateDTO(
                    UUID.randomUUID(),
                    veiculo.getId(),
                    "Queixa",
                    null,
                    1000
            );
            when(clienteRepository.findById(dto.clienteId())).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> ordemServicoService.criar(dto, atendente.getEmail()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cliente não encontrado com ID: " + dto.clienteId());

            verify(ordemServicoRepository, never()).save(any(OrdemServico.class));
            verify(historicoStatusOsRepository, never()).save(any(HistoricoStatusOS.class));
        }

        @Test
        @DisplayName("deve lançar exceção quando veículo está inativo ou não é encontrado")
        void deveLancarExcecaoQuandoVeiculoEstaInativoOuNaoEEncontrado() {
            // Given
            OrdemServicoCreateDTO dto = new OrdemServicoCreateDTO(
                    cliente.getId(),
                    UUID.randomUUID(),
                    "Queixa",
                    null,
                    1000
            );
            when(clienteRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
            when(veiculoRepository.findByIdAndAtivoTrue(dto.veiculoId())).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> ordemServicoService.criar(dto, atendente.getEmail()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Veículo inativo ou não encontrado com ID: " + dto.veiculoId());

            verify(ordemServicoRepository, never()).save(any(OrdemServico.class));
            verify(historicoStatusOsRepository, never()).save(any(HistoricoStatusOS.class));
        }
    }

    @Nested
    @DisplayName("Testes do método avancarStatus()")
    class AvancarStatusTests {

        @ParameterizedTest(name = "deve avançar de {0} para {1} com sucesso")
        @MethodSource("provideValidStatusTransitions")
        void deveAvancarCadaTransicaoValidaComSucesso(StatusOS statusAtual,
                                                      StatusOS novoStatus,
                                                      boolean deveDefinirInicio,
                                                      boolean deveDefinirFinalizacao,
                                                      boolean deveDefinirEntrega) {
            // Given
            OrdemServico ordemServico = ordemServico(statusAtual);
            OrdemServicoResponseDTO response = responseDTO(novoStatus, ordemServico.getNumero());
            AvancarStatusDTO dto = new AvancarStatusDTO(novoStatus.name(), "Observação da transição");

            when(ordemServicoRepository.findById(ordemServico.getId())).thenReturn(Optional.of(ordemServico));
            when(ordemServicoRepository.save(ordemServico)).thenReturn(ordemServico);
            when(funcionarioRepository.findByEmail(mecanico.getEmail())).thenReturn(Optional.of(mecanico));
            when(ordemServicoMapper.toResponse(ordemServico)).thenReturn(response);

            // When
            OrdemServicoResponseDTO resultado = ordemServicoService.avancarStatus(ordemServico.getId(), dto, mecanico.getEmail());

            // Then
            assertThat(resultado).isEqualTo(response);
            assertThat(ordemServico.getStatus()).isEqualTo(novoStatus);
            if (deveDefinirInicio) {
                assertThat(ordemServico.getDataInicioExecucao()).isNotNull();
            }
            if (deveDefinirFinalizacao) {
                assertThat(ordemServico.getDataFinalizacao()).isNotNull();
            }
            if (deveDefinirEntrega) {
                assertThat(ordemServico.getDataEntrega()).isNotNull();
            }

            ArgumentCaptor<HistoricoStatusOS> historicoCaptor = ArgumentCaptor.forClass(HistoricoStatusOS.class);
            verify(historicoStatusOsRepository).save(historicoCaptor.capture());
            HistoricoStatusOS historico = historicoCaptor.getValue();
            assertThat(historico.getStatusAnterior()).isEqualTo(statusAtual);
            assertThat(historico.getStatusNovo()).isEqualTo(novoStatus);
            assertThat(historico.getObservacao()).isEqualTo("Observação da transição");
            assertThat(historico.getExecutadoPor()).isEqualTo(mecanico);
        }

        private static Stream<Arguments> provideValidStatusTransitions() {
            return Stream.of(
                    Arguments.of(StatusOS.RECEBIDA, StatusOS.EM_DIAGNOSTICO, false, false, false),
                    Arguments.of(StatusOS.EM_DIAGNOSTICO, StatusOS.AGUARDANDO_APROVACAO, false, false, false),
                    Arguments.of(StatusOS.AGUARDANDO_APROVACAO, StatusOS.EM_EXECUCAO, true, false, false),
                    Arguments.of(StatusOS.AGUARDANDO_APROVACAO, StatusOS.EM_DIAGNOSTICO, false, false, false),
                    Arguments.of(StatusOS.EM_EXECUCAO, StatusOS.FINALIZADA, false, true, false),
                    Arguments.of(StatusOS.FINALIZADA, StatusOS.ENTREGUE, false, false, true)
            );
        }

        @Test
        @DisplayName("deve lançar exceção de negócio quando a transição é inválida")
        void deveLancarExcecaoDeNegocioQuandoATransicaoEInvalida() {
            // Given
            OrdemServico ordemServico = ordemServico(StatusOS.RECEBIDA);
            AvancarStatusDTO dto = new AvancarStatusDTO(StatusOS.FINALIZADA.name(), "Pular etapas");
            when(ordemServicoRepository.findById(ordemServico.getId())).thenReturn(Optional.of(ordemServico));

            // When / Then
            assertThatThrownBy(() -> ordemServicoService.avancarStatus(ordemServico.getId(), dto, mecanico.getEmail()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Transição de status inválida: RECEBIDA → FINALIZADA");

            verify(ordemServicoRepository, never()).save(any(OrdemServico.class));
            verify(historicoStatusOsRepository, never()).save(any(HistoricoStatusOS.class));
        }

        @Test
        @DisplayName("deve lançar exceção quando a ordem de serviço não é encontrada")
        void deveLancarExcecaoQuandoAOrdemDeServicoNaoEEncontrada() {
            // Given
            UUID id = UUID.randomUUID();
            when(ordemServicoRepository.findById(id)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> ordemServicoService.avancarStatus(id, new AvancarStatusDTO("EM_DIAGNOSTICO", null), mecanico.getEmail()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Ordem de serviço não encontrada com ID: " + id);
        }
    }

    @Nested
    @DisplayName("Testes do método registrarDiagnostico()")
    class RegistrarDiagnosticoTests {

        @Test
        @DisplayName("deve registrar diagnóstico quando a OS está em diagnóstico")
        void deveRegistrarDiagnosticoQuandoAOsEstaEmDiagnostico() {
            // Given
            OrdemServico ordemServico = ordemServico(StatusOS.EM_DIAGNOSTICO);
            DiagnosticoDTO dto = new DiagnosticoDTO("Falha no conjunto de freios traseiros");
            OrdemServicoResponseDTO response = responseDTO(StatusOS.EM_DIAGNOSTICO, ordemServico.getNumero());

            when(ordemServicoRepository.findById(ordemServico.getId())).thenReturn(Optional.of(ordemServico));
            when(ordemServicoRepository.save(ordemServico)).thenReturn(ordemServico);
            when(ordemServicoMapper.toResponse(ordemServico)).thenReturn(response);

            // When
            OrdemServicoResponseDTO resultado = ordemServicoService.registrarDiagnostico(ordemServico.getId(), dto);

            // Then
            assertThat(resultado).isEqualTo(response);
            ArgumentCaptor<OrdemServico> ordemServicoCaptor = ArgumentCaptor.forClass(OrdemServico.class);
            verify(ordemServicoRepository).save(ordemServicoCaptor.capture());
            assertThat(ordemServicoCaptor.getValue().getDiagnostico()).isEqualTo("Falha no conjunto de freios traseiros");
        }

        @Test
        @DisplayName("deve lançar exceção quando a OS não está em diagnóstico")
        void deveLancarExcecaoQuandoAOsNaoEstaEmDiagnostico() {
            // Given
            OrdemServico ordemServico = ordemServico(StatusOS.RECEBIDA);
            when(ordemServicoRepository.findById(ordemServico.getId())).thenReturn(Optional.of(ordemServico));

            // When / Then
            assertThatThrownBy(() -> ordemServicoService.registrarDiagnostico(ordemServico.getId(), new DiagnosticoDTO("Teste")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Diagnóstico só pode ser registrado quando a OS está em EM_DIAGNOSTICO. Status atual: RECEBIDA");

            verify(ordemServicoRepository, never()).save(any(OrdemServico.class));
        }
    }

    @Nested
    @DisplayName("Testes do método atribuirMecanico()")
    class AtribuirMecanicoTests {

        @Test
        @DisplayName("deve atribuir mecânico quando o funcionário informado é um mecânico")
        void deveAtribuirMecanicoQuandoOFuncionarioInformadoEMecanico() {
            // Given
            OrdemServico ordemServico = ordemServico(StatusOS.RECEBIDA);
            OrdemServicoResponseDTO response = responseDTO(StatusOS.RECEBIDA, ordemServico.getNumero());
            AtribuirMecanicoDTO dto = new AtribuirMecanicoDTO(mecanico.getId());

            when(ordemServicoRepository.findById(ordemServico.getId())).thenReturn(Optional.of(ordemServico));
            when(funcionarioRepository.findById(mecanico.getId())).thenReturn(Optional.of(mecanico));
            when(ordemServicoRepository.save(ordemServico)).thenReturn(ordemServico);
            when(ordemServicoMapper.toResponse(ordemServico)).thenReturn(response);

            // When
            OrdemServicoResponseDTO resultado = ordemServicoService.atribuirMecanico(ordemServico.getId(), dto);

            // Then
            assertThat(resultado).isEqualTo(response);
            ArgumentCaptor<OrdemServico> ordemServicoCaptor = ArgumentCaptor.forClass(OrdemServico.class);
            verify(ordemServicoRepository).save(ordemServicoCaptor.capture());
            assertThat(ordemServicoCaptor.getValue().getMecanicoResponsavel()).isEqualTo(mecanico);
        }

        @Test
        @DisplayName("deve lançar exceção quando o funcionário informado não é mecânico")
        void deveLancarExcecaoQuandoOFuncionarioInformadoNaoEMecanico() {
            // Given
            OrdemServico ordemServico = ordemServico(StatusOS.RECEBIDA);
            Funcionario funcionario = atendente;

            when(ordemServicoRepository.findById(ordemServico.getId())).thenReturn(Optional.of(ordemServico));
            when(funcionarioRepository.findById(funcionario.getId())).thenReturn(Optional.of(funcionario));

            // When / Then
            assertThatThrownBy(() -> ordemServicoService.atribuirMecanico(ordemServico.getId(), new AtribuirMecanicoDTO(funcionario.getId())))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("O funcionário informado não é um mecânico.");

            verify(ordemServicoRepository, never()).save(any(OrdemServico.class));
        }
    }

    @Nested
    @DisplayName("Testes do método listar()")
    class ListarTests {

        private final Pageable pageable = PageRequest.of(0, 20);

        @Test
        @DisplayName("deve listar por status quando o filtro de status é informado")
        void deveListarPorStatusQuandoOFiltroDeStatusEInformado() {
            // Given
            OrdemServico ordemServico = ordemServico(StatusOS.EM_DIAGNOSTICO);
            OrdemServicoResponseDTO response = responseDTO(StatusOS.EM_DIAGNOSTICO, ordemServico.getNumero());
            when(ordemServicoRepository.findByStatus(StatusOS.EM_DIAGNOSTICO, pageable)).thenReturn(new PageImpl<>(List.of(ordemServico)));
            when(ordemServicoMapper.toResponse(ordemServico)).thenReturn(response);

            // When
            Page<OrdemServicoResponseDTO> resultado = ordemServicoService.listar(StatusOS.EM_DIAGNOSTICO, null, null, pageable);

            // Then
            assertThat(resultado.getContent()).containsExactly(response);
            verify(ordemServicoRepository).findByStatus(StatusOS.EM_DIAGNOSTICO, pageable);
        }

        @Test
        @DisplayName("deve listar por cliente quando o filtro de cliente é informado")
        void deveListarPorClienteQuandoOFiltroDeClienteEInformado() {
            // Given
            OrdemServico ordemServico = ordemServico(StatusOS.RECEBIDA);
            OrdemServicoResponseDTO response = responseDTO(StatusOS.RECEBIDA, ordemServico.getNumero());
            when(ordemServicoRepository.findByClienteId(cliente.getId(), pageable)).thenReturn(new PageImpl<>(List.of(ordemServico)));
            when(ordemServicoMapper.toResponse(ordemServico)).thenReturn(response);

            // When
            Page<OrdemServicoResponseDTO> resultado = ordemServicoService.listar(null, cliente.getId(), null, pageable);

            // Then
            assertThat(resultado.getContent()).containsExactly(response);
            verify(ordemServicoRepository).findByClienteId(cliente.getId(), pageable);
        }

        @Test
        @DisplayName("deve listar por mecânico quando o filtro de mecânico é informado")
        void deveListarPorMecanicoQuandoOFiltroDeMecanicoEInformado() {
            // Given
            OrdemServico ordemServico = ordemServico(StatusOS.EM_EXECUCAO);
            ordemServico.setMecanicoResponsavel(mecanico);
            OrdemServicoResponseDTO response = responseDTO(StatusOS.EM_EXECUCAO, ordemServico.getNumero());
            when(ordemServicoRepository.findByMecanicoResponsavelId(mecanico.getId(), pageable)).thenReturn(new PageImpl<>(List.of(ordemServico)));
            when(ordemServicoMapper.toResponse(ordemServico)).thenReturn(response);

            // When
            Page<OrdemServicoResponseDTO> resultado = ordemServicoService.listar(null, null, mecanico.getId(), pageable);

            // Then
            assertThat(resultado.getContent()).containsExactly(response);
            verify(ordemServicoRepository).findByMecanicoResponsavelId(mecanico.getId(), pageable);
        }

        @Test
        @DisplayName("deve listar todas as ordens quando nenhum filtro é informado")
        void deveListarTodasAsOrdensQuandoNenhumFiltroEInformado() {
            // Given
            OrdemServico ordemServico = ordemServico(StatusOS.RECEBIDA);
            OrdemServicoResponseDTO response = responseDTO(StatusOS.RECEBIDA, ordemServico.getNumero());
            when(ordemServicoRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(ordemServico)));
            when(ordemServicoMapper.toResponse(ordemServico)).thenReturn(response);

            // When
            Page<OrdemServicoResponseDTO> resultado = ordemServicoService.listar(null, null, null, pageable);

            // Then
            assertThat(resultado.getContent()).containsExactly(response);
            verify(ordemServicoRepository).findAll(pageable);
        }
    }

    private OrdemServico ordemServico(StatusOS status) {
        return OrdemServico.builder()
                .id(UUID.randomUUID())
                .numero("OS-000123")
                .status(status)
                .cliente(cliente)
                .veiculo(veiculo)
                .atendenteRecepcao(atendente)
                .queixaCliente("Barulho no motor")
                .valorTotalAprovado(BigDecimal.ZERO)
                .build();
    }

    private OrdemServicoResponseDTO responseDTO(StatusOS status, String numero) {
        return new OrdemServicoResponseDTO(
                UUID.randomUUID(),
                numero,
                status.name(),
                "Barulho no motor",
                null,
                BigDecimal.ZERO,
                null,
                cliente.getId(),
                cliente.getNome(),
                veiculo.getId(),
                veiculo.getPlaca()
        );
    }
}

