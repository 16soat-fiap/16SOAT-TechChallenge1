package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.MovimentacaoEstoque;
import com.autopecas.autopecas.domain.entity.Peca;
import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.dto.peca.MovimentacaoCreateDTO;
import com.autopecas.autopecas.dto.peca.MovimentacaoResponseDTO;
import com.autopecas.autopecas.dto.peca.PecaCreateDTO;
import com.autopecas.autopecas.dto.peca.PecaResponseDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.PecaMapper;
import com.autopecas.autopecas.repository.PecaRepository;
import com.autopecas.autopecas.util.test.PecaBuilder;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("PecaService")
class PecaServiceTest {

    @Mock
    private PecaRepository pecaRepository;
    @Mock
    private EstoqueService estoqueService;
    @Mock
    private PecaMapper pecaMapper;

    @InjectMocks
    private PecaService pecaService;

    private Peca peca;

    @BeforeEach
    void setUp() {
        peca = PecaBuilder.peca().build();
    }

    @Nested
    @DisplayName("Testes de consulta")
    class ConsultaTests {

        @Test
        @DisplayName("deve listar peças ativas")
        void deveListarPecasAtivas() {
            // Given
            PecaResponseDTO response = responseDTO(peca);
            when(pecaRepository.findByAtivoTrue()).thenReturn(List.of(peca));
            when(pecaMapper.toResponse(peca)).thenReturn(response);

            // When
            List<PecaResponseDTO> resultado = pecaService.listar(false);

            // Then
            assertThat(resultado).containsExactly(response);
        }

        @Test
        @DisplayName("deve listar peças com estoque baixo quando filtro é verdadeiro")
        void deveListarPecasComEstoqueBaixoQuandoFiltroEVerdadeiro() {
            // Given
            PecaResponseDTO response = responseDTO(peca);
            when(pecaRepository.findEstoqueBaixo()).thenReturn(List.of(peca));
            when(pecaMapper.toResponse(peca)).thenReturn(response);

            // When
            List<PecaResponseDTO> resultado = pecaService.listar(true);

            // Then
            assertThat(resultado).containsExactly(response);
            verify(pecaRepository).findEstoqueBaixo();
        }

        @Test
        @DisplayName("deve buscar peça por id")
        void deveBuscarPecaPorId() {
            // Given
            PecaResponseDTO response = responseDTO(peca);
            when(pecaRepository.findById(peca.getId())).thenReturn(Optional.of(peca));
            when(pecaMapper.toResponse(peca)).thenReturn(response);

            // When
            PecaResponseDTO resultado = pecaService.buscarPorId(peca.getId());

            // Then
            assertThat(resultado).isEqualTo(response);
        }

        @Test
        @DisplayName("deve lançar exceção ao buscar peça inexistente por id")
        void deveLancarExcecaoAoBuscarPecaInexistentePorId() {
            // Given
            UUID id = UUID.randomUUID();
            when(pecaRepository.findById(id)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> pecaService.buscarPorId(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Id " + id + " não encontrado");
        }

        @Test
        @DisplayName("deve buscar peça por código")
        void deveBuscarPecaPorCodigo() {
            // Given
            PecaResponseDTO response = responseDTO(peca);
            when(pecaRepository.findByCodigo(peca.getCodigo())).thenReturn(Optional.of(peca));
            when(pecaMapper.toResponse(peca)).thenReturn(response);

            // When
            PecaResponseDTO resultado = pecaService.buscarPorCodigo(peca.getCodigo());

            // Then
            assertThat(resultado).isEqualTo(response);
        }
    }

    @Nested
    @DisplayName("Testes de criação e atualização")
    class MutacaoTests {

        @Test
        @DisplayName("deve criar peça e registrar estoque inicial quando informado")
        void deveCriarPecaERegistrarEstoqueInicialQuandoInformado() {
            // Given
            PecaCreateDTO dto = new PecaCreateDTO("PEC900", "Pastilha", "Dianteira", new BigDecimal("99.90"), 8, 2, "kit");
            PecaResponseDTO response = responseDTO(peca);

            when(pecaRepository.existsByCodigo(dto.codigo())).thenReturn(false);
            when(pecaRepository.save(any(Peca.class))).thenReturn(peca);
            when(pecaMapper.toResponse(peca)).thenReturn(response);

            // When
            PecaResponseDTO resultado = pecaService.criar(dto);

            // Then
            assertThat(resultado).isEqualTo(response);
            ArgumentCaptor<Peca> pecaCaptor = ArgumentCaptor.forClass(Peca.class);
            verify(pecaRepository).save(pecaCaptor.capture());
            assertThat(pecaCaptor.getValue().getCodigo()).isEqualTo("PEC900");
            assertThat(pecaCaptor.getValue().getQuantidadeMinima()).isEqualTo(2);
            assertThat(pecaCaptor.getValue().getUnidade()).isEqualTo("kit");
            verify(estoqueService).registrarMovimentacao(peca, TipoMovimentacaoEstoque.ENTRADA, 8, "Estoque inicial na criação da peça", null, null);
        }

        @Test
        @DisplayName("deve bloquear criação de peça com código duplicado")
        void deveBloquearCriacaoDePecaComCodigoDuplicado() {
            // Given
            PecaCreateDTO dto = new PecaCreateDTO(peca.getCodigo(), "Pastilha", null, BigDecimal.TEN, null, null, null);
            when(pecaRepository.existsByCodigo(dto.codigo())).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> pecaService.criar(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Código da peça já está cadastrado: " + dto.codigo());
        }

        @Test
        @DisplayName("deve atualizar peça")
        void deveAtualizarPeca() {
            // Given
            PecaCreateDTO dto = new PecaCreateDTO("IGNORADO", "Pastilha Premium", "Nova descrição", new BigDecimal("120.00"), null, 5, "par");
            PecaResponseDTO response = responseDTO(peca);

            when(pecaRepository.findById(peca.getId())).thenReturn(Optional.of(peca));
            when(pecaRepository.save(peca)).thenReturn(peca);
            when(pecaMapper.toResponse(peca)).thenReturn(response);

            // When
            PecaResponseDTO resultado = pecaService.atualizar(peca.getId(), dto);

            // Then
            assertThat(resultado).isEqualTo(response);
            assertThat(peca.getNome()).isEqualTo("Pastilha Premium");
            assertThat(peca.getDescricao()).isEqualTo("Nova descrição");
            assertThat(peca.getPrecoVenda()).isEqualByComparingTo(new BigDecimal("120.00"));
            assertThat(peca.getQuantidadeMinima()).isEqualTo(5);
            assertThat(peca.getUnidade()).isEqualTo("par");
        }

        @Test
        @DisplayName("deve lançar exceção ao atualizar peça inexistente")
        void deveLancarExcecaoAoAtualizarPecaInexistente() {
            // Given
            UUID id = UUID.randomUUID();
            when(pecaRepository.findById(id)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> pecaService.atualizar(id, new PecaCreateDTO("A", "B", null, BigDecimal.ONE, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Peça não encontrada");
        }

        @Test
        @DisplayName("deve desativar peça")
        void deveDesativarPeca() {
            // Given
            when(pecaRepository.findById(peca.getId())).thenReturn(Optional.of(peca));

            // When
            pecaService.desativar(peca.getId());

            // Then
            assertThat(peca.getAtivo()).isFalse();
            verify(pecaRepository).save(peca);
        }
    }

    @Nested
    @DisplayName("Testes de movimentação")
    class MovimentacaoTests {

        @Test
        @DisplayName("deve registrar movimentação manual de peça")
        void deveRegistrarMovimentacaoManualDePeca() {
            // Given
            MovimentacaoCreateDTO dto = new MovimentacaoCreateDTO("entrada", 4, "Reposição");
            MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                    .id(UUID.randomUUID())
                    .tipo(TipoMovimentacaoEstoque.ENTRADA)
                    .quantidade(4)
                    .saldoApos(14)
                    .motivo("Reposição")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(pecaRepository.findById(peca.getId())).thenReturn(Optional.of(peca));
            when(estoqueService.registrarMovimentacao(peca, TipoMovimentacaoEstoque.ENTRADA, 4, "Reposição", null, null)).thenReturn(movimentacao);

            // When
            MovimentacaoResponseDTO resultado = pecaService.registrarMovimentacao(peca.getId(), dto);

            // Then
            assertThat(resultado.tipo()).isEqualTo("ENTRADA");
            assertThat(resultado.quantidade()).isEqualTo(4);
            assertThat(resultado.saldoApos()).isEqualTo(14);
        }

        @Test
        @DisplayName("deve lançar exceção para tipo de movimentação inválido")
        void deveLancarExcecaoParaTipoDeMovimentacaoInvalido() {
            // Given
            when(pecaRepository.findById(peca.getId())).thenReturn(Optional.of(peca));

            // When / Then
            assertThatThrownBy(() -> pecaService.registrarMovimentacao(peca.getId(), new MovimentacaoCreateDTO("errado", 1, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Tipo de movimentação inválido: errado");
        }
    }

    private PecaResponseDTO responseDTO(Peca peca) {
        return new PecaResponseDTO(peca.getId(), peca.getCodigo(), peca.getNome(), peca.getDescricao(), peca.getMarca(), peca.getPrecoVenda(), peca.getQuantidadeEstoque(), peca.getQuantidadeMinima(), peca.getUnidade(), peca.getAtivo(), null, null, peca.estoqueBaixo());
    }
}

