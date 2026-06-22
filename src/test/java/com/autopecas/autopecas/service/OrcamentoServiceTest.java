package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.domain.entity.ItemOrcamentoPeca;
import com.autopecas.autopecas.domain.entity.ItemOrcamentoServico;
import com.autopecas.autopecas.domain.entity.MovimentacaoEstoque;
import com.autopecas.autopecas.domain.entity.Orcamento;
import com.autopecas.autopecas.domain.entity.OrdemServico;
import com.autopecas.autopecas.domain.entity.Peca;
import com.autopecas.autopecas.domain.entity.Servico;
import com.autopecas.autopecas.domain.entity.Veiculo;
import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.domain.enums.StatusOrcamento;
import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.dto.orcamento.AprovarRejeitarDTO;
import com.autopecas.autopecas.dto.orcamento.OrcamentoCreateDTO;
import com.autopecas.autopecas.dto.orcamento.OrcamentoResponseDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.mapper.OrcamentoMapper;
import com.autopecas.autopecas.repository.OrcamentoRepository;
import com.autopecas.autopecas.repository.OrdemServicoRepository;
import com.autopecas.autopecas.repository.PecaRepository;
import com.autopecas.autopecas.repository.ServicoRepository;
import com.autopecas.autopecas.util.test.ClienteBuilder;
import com.autopecas.autopecas.util.test.FuncionarioBuilder;
import com.autopecas.autopecas.util.test.OrcamentoBuilder;
import com.autopecas.autopecas.util.test.PecaBuilder;
import com.autopecas.autopecas.util.test.ServicoBuilder;
import com.autopecas.autopecas.util.test.VeiculoBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrcamentoService")
class OrcamentoServiceTest {

    @Mock
    private OrcamentoRepository orcamentoRepository;
    @Mock
    private OrdemServicoRepository ordemServicoRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private PecaRepository pecaRepository;
    @Mock
    private EstoqueService estoqueService;
    @Mock
    private OrcamentoMapper orcamentoMapper;

    @InjectMocks
    private OrcamentoService orcamentoService;

    private Cliente cliente;
    private Veiculo veiculo;
    private OrdemServico ordemServico;
    private Servico servico;
    private Peca peca;

    @BeforeEach
    void setUp() {
        cliente = ClienteBuilder.clientePF().build();
        veiculo = VeiculoBuilder.veiculo(cliente).build();
        ordemServico = OrdemServico.builder()
                .id(UUID.randomUUID())
                .numero("OS-000321")
                .status(StatusOS.AGUARDANDO_APROVACAO)
                .cliente(cliente)
                .veiculo(veiculo)
                .atendenteRecepcao(FuncionarioBuilder.atendente().build())
                .queixaCliente("Vazamento")
                .valorTotalAprovado(BigDecimal.ZERO)
                .build();
        servico = ServicoBuilder.servico().precoBase(new BigDecimal("150.00")).build();
        peca = PecaBuilder.peca().precoVenda(new BigDecimal("80.00")).build();
    }

    @Nested
    @DisplayName("Testes do método criarOrcamento()")
    class CriarOrcamentoTests {

        @Test
        @DisplayName("deve criar orçamento a partir de uma ordem de serviço")
        void deveCriarOrcamentoAPartirDeUmaOrdemDeServico() {
            // Given
            OrcamentoCreateDTO dto = new OrcamentoCreateDTO(
                    List.of(new OrcamentoCreateDTO.ItemServicoDTO(servico.getId(), 2)),
                    List.of(new OrcamentoCreateDTO.ItemPecaDTO(peca.getId(), 3)),
                    "Cartão em 3x",
                    4,
                    LocalDate.now().plusDays(7),
                    "Trocar conjunto completo"
            );
            OrcamentoResponseDTO response = responseDTO(StatusOrcamento.RASCUNHO, new BigDecimal("540.00"));

            when(ordemServicoRepository.findById(ordemServico.getId())).thenReturn(Optional.of(ordemServico));
            when(orcamentoRepository.existsByOrdemServicoIdAndStatus(ordemServico.getId(), StatusOrcamento.APROVADA)).thenReturn(false);
            when(servicoRepository.findById(servico.getId())).thenReturn(Optional.of(servico));
            when(pecaRepository.findById(peca.getId())).thenReturn(Optional.of(peca));
            when(ordemServicoRepository.save(ordemServico)).thenReturn(ordemServico);
            when(orcamentoMapper.toResponse(any(Orcamento.class))).thenReturn(response);

            // When
            OrcamentoResponseDTO resultado = orcamentoService.criarOrcamento(ordemServico.getId(), dto);

            // Then
            assertThat(resultado).isEqualTo(response);
            assertThat(ordemServico.getOrcamentos()).hasSize(1);

            Orcamento orcamentoCriado = ordemServico.getOrcamentos().getFirst();
            assertThat(orcamentoCriado.getVersao()).isEqualTo(1);
            assertThat(orcamentoCriado.getStatus()).isEqualTo(StatusOrcamento.RASCUNHO);
            assertThat(orcamentoCriado.getCondicoesPagamento()).isEqualTo("Cartão em 3x");
            assertThat(orcamentoCriado.getPrazoExecucaoDias()).isEqualTo(4);
            assertThat(orcamentoCriado.getObservacoes()).isEqualTo("Trocar conjunto completo");
            assertThat(orcamentoCriado.getItensServico()).hasSize(1);
            assertThat(orcamentoCriado.getItensPeca()).hasSize(1);
            assertThat(orcamentoCriado.getValorMaoObra()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(orcamentoCriado.getValorPecas()).isEqualByComparingTo(new BigDecimal("240.00"));
            assertThat(orcamentoCriado.getValorTotal()).isEqualByComparingTo(new BigDecimal("540.00"));

            verify(ordemServicoRepository).save(ordemServico);
            verify(servicoRepository).findById(servico.getId());
            verify(pecaRepository).findById(peca.getId());
        }
    }

    @Nested
    @DisplayName("Testes do método enviar()")
    class EnviarTests {

        @Test
        @DisplayName("deve enviar orçamento e alterar seu status para ENVIADA")
        void deveEnviarOrcamentoEAlterarSeuStatusParaEnviada() {
            // Given
            Orcamento orcamento = OrcamentoBuilder.orcamento(ordemServico).status(StatusOrcamento.RASCUNHO).build();
            OrcamentoResponseDTO response = responseDTO(StatusOrcamento.ENVIADA, BigDecimal.ZERO);

            when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
            when(orcamentoRepository.save(orcamento)).thenReturn(orcamento);
            when(orcamentoMapper.toResponse(orcamento)).thenReturn(response);

            // When
            OrcamentoResponseDTO resultado = orcamentoService.enviar(ordemServico.getId(), orcamento.getId());

            // Then
            assertThat(resultado).isEqualTo(response);
            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.ENVIADA);
            assertThat(orcamento.getDataEnvio()).isNotNull();
            verify(ordemServicoRepository, never()).save(any(OrdemServico.class));
        }
    }

    @Nested
    @DisplayName("Testes do método aprovar()")
    class AprovarTests {

        @Test
        @DisplayName("deve aprovar orçamento, avançar OS para EM_EXECUCAO e registrar saída de estoque")
        void deveAprovarOrcamentoAvancarOsParaEmExecucaoERegistrarSaidaDeEstoque() {
            // Given
            Orcamento orcamento = OrcamentoBuilder.orcamento(ordemServico)
                    .status(StatusOrcamento.ENVIADA)
                    .valorTotal(new BigDecimal("230.00"))
                    .build();

            ItemOrcamentoServico itemServico = ItemOrcamentoServico.builder()
                    .orcamento(orcamento)
                    .servico(servico)
                    .quantidade(1)
                    .precoUnitario(new BigDecimal("150.00"))
                    .observacao("Troca da correia")
                    .build();
            ItemOrcamentoPeca itemPeca = ItemOrcamentoPeca.builder()
                    .orcamento(orcamento)
                    .peca(peca)
                    .quantidade(1)
                    .precoUnitario(new BigDecimal("80.00"))
                    .build();
            orcamento.getItensServico().add(itemServico);
            orcamento.getItensPeca().add(itemPeca);
            OrcamentoResponseDTO response = responseDTO(StatusOrcamento.APROVADA, new BigDecimal("230.00"));

            when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
            when(orcamentoRepository.existsByOrdemServicoIdAndStatus(ordemServico.getId(), StatusOrcamento.APROVADA)).thenReturn(false);
            when(orcamentoMapper.toResponse(orcamento)).thenReturn(response);
            when(estoqueService.registrarMovimentacao(eq(peca), eq(TipoMovimentacaoEstoque.SAIDA), eq(1), any(String.class), eq(ordemServico), eq(null)))
                    .thenReturn(MovimentacaoEstoque.builder().peca(peca).tipo(TipoMovimentacaoEstoque.SAIDA).quantidade(1).saldoApos(9).build());

            // When
            OrcamentoResponseDTO resultado = orcamentoService.aprovar(ordemServico.getId(), orcamento.getId());

            // Then
            assertThat(resultado).isEqualTo(response);
            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.APROVADA);
            assertThat(ordemServico.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
            assertThat(ordemServico.getValorTotalAprovado()).isEqualByComparingTo(new BigDecimal("230.00"));
            assertThat(ordemServico.getItensServico()).hasSize(1);
            assertThat(ordemServico.getItensPeca()).hasSize(1);
            assertThat(ordemServico.getItensServico().getFirst().getServico()).isEqualTo(servico);
            assertThat(ordemServico.getItensPeca().getFirst().getPeca()).isEqualTo(peca);

            verify(orcamentoRepository).save(orcamento);
            verify(ordemServicoRepository).save(ordemServico);
            verify(estoqueService).registrarMovimentacao(
                    peca,
                    TipoMovimentacaoEstoque.SAIDA,
                    1,
                    "Saída por aprovação do orçamento da OS " + ordemServico.getNumero(),
                    ordemServico,
                    null
            );
        }

        @Test
        @DisplayName("deve bloquear aprovação quando já existe orçamento aprovado para a OS")
        void deveBloquearAprovacaoQuandoJaExisteOrcamentoAprovadoParaOs() {
            // Given
            Orcamento orcamento = OrcamentoBuilder.orcamento(ordemServico).status(StatusOrcamento.ENVIADA).build();
            when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
            when(orcamentoRepository.existsByOrdemServicoIdAndStatus(ordemServico.getId(), StatusOrcamento.APROVADA)).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> orcamentoService.aprovar(ordemServico.getId(), orcamento.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Já existe um orçamento aprovado para esta OS.");

            verify(orcamentoRepository, never()).save(any(Orcamento.class));
            verify(ordemServicoRepository, never()).save(any(OrdemServico.class));
            verify(estoqueService, never()).registrarMovimentacao(any(), any(), any(Integer.class), any(String.class), any(), any());
        }
    }

    @Nested
    @DisplayName("Testes do método rejeitar()")
    class RejeitarTests {

        @Test
        @DisplayName("deve rejeitar orçamento e permitir criação de nova versão para a mesma OS")
        void deveRejeitarOrcamentoEPermitirCriacaoDeNovaVersaoParaMesmaOs() {
            // Given
            Orcamento orcamentoEnviado = OrcamentoBuilder.orcamento(ordemServico).status(StatusOrcamento.ENVIADA).versao(1).build();
            ordemServico.getOrcamentos().add(orcamentoEnviado);
            OrcamentoResponseDTO rejeitadoResponse = responseDTO(StatusOrcamento.CANCELADA, BigDecimal.ZERO);
            OrcamentoCreateDTO novoOrcamentoDTO = new OrcamentoCreateDTO(
                    List.of(new OrcamentoCreateDTO.ItemServicoDTO(servico.getId(), 1)),
                    null,
                    "Pix",
                    2,
                    LocalDate.now().plusDays(3),
                    "Nova proposta"
            );
            OrcamentoResponseDTO novoResponse = responseDTO(StatusOrcamento.RASCUNHO, new BigDecimal("150.00"));

            when(orcamentoRepository.findById(orcamentoEnviado.getId())).thenReturn(Optional.of(orcamentoEnviado));
            when(orcamentoRepository.save(orcamentoEnviado)).thenReturn(orcamentoEnviado);

            when(ordemServicoRepository.findById(ordemServico.getId())).thenReturn(Optional.of(ordemServico));
            when(orcamentoRepository.existsByOrdemServicoIdAndStatus(ordemServico.getId(), StatusOrcamento.APROVADA)).thenReturn(false);
            when(servicoRepository.findById(servico.getId())).thenReturn(Optional.of(servico));
            when(ordemServicoRepository.save(ordemServico)).thenReturn(ordemServico);
            when(orcamentoMapper.toResponse(any(Orcamento.class))).thenAnswer(invocation -> {
                Orcamento orcamento = invocation.getArgument(0);
                return orcamento == orcamentoEnviado ? rejeitadoResponse : novoResponse;
            });

            // When
            OrcamentoResponseDTO rejeitado = orcamentoService.rejeitar(ordemServico.getId(), orcamentoEnviado.getId(), new AprovarRejeitarDTO("Cliente não aprovou"));
            OrcamentoResponseDTO novo = orcamentoService.criarOrcamento(ordemServico.getId(), novoOrcamentoDTO);

            // Then
            assertThat(rejeitado).isEqualTo(rejeitadoResponse);
            assertThat(novo).isEqualTo(novoResponse);
            assertThat(orcamentoEnviado.getStatus()).isEqualTo(StatusOrcamento.CANCELADA);
            assertThat(orcamentoEnviado.getMotivoRejeicao()).isEqualTo("Cliente não aprovou");
            assertThat(ordemServico.getOrcamentos()).hasSize(2);
            assertThat(ordemServico.getOrcamentos().get(1).getVersao()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Testes auxiliares de listagem")
    class ListarTests {

        @Test
        @DisplayName("deve listar orçamentos da OS")
        void deveListarOrcamentosDaOs() {
            // Given
            Orcamento orcamento = OrcamentoBuilder.orcamento(ordemServico).build();
            OrcamentoResponseDTO response = responseDTO(StatusOrcamento.RASCUNHO, BigDecimal.ZERO);
            when(ordemServicoRepository.existsById(ordemServico.getId())).thenReturn(true);
            when(orcamentoRepository.findByOrdemServicoId(ordemServico.getId())).thenReturn(List.of(orcamento));
            when(orcamentoMapper.toResponse(orcamento)).thenReturn(response);

            // When
            List<OrcamentoResponseDTO> resultado = orcamentoService.listar(ordemServico.getId());

            // Then
            assertThat(resultado).containsExactly(response);
        }
    }

    private OrcamentoResponseDTO responseDTO(StatusOrcamento status, BigDecimal valorTotal) {
        return new OrcamentoResponseDTO(
                UUID.randomUUID(),
                1,
                status.name(),
                new BigDecimal("150.00"),
                valorTotal.compareTo(BigDecimal.ZERO) > 0 ? valorTotal.subtract(new BigDecimal("150.00")) : BigDecimal.ZERO,
                valorTotal,
                "Condição",
                3,
                LocalDate.now().plusDays(5),
                null
        );
    }
}

