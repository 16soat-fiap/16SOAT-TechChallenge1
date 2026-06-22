package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Servico;
import com.autopecas.autopecas.dto.servico.ServicoCreateDTO;
import com.autopecas.autopecas.dto.servico.ServicoResponseDTO;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.ServicoMapper;
import com.autopecas.autopecas.repository.ServicoRepository;
import com.autopecas.autopecas.util.test.ServicoBuilder;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServicoService")
class ServicoServiceTest {

    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private ServicoMapper servicoMapper;

    @InjectMocks
    private ServicoService servicoService;

    private Servico servico;

    @BeforeEach
    void setUp() {
        servico = ServicoBuilder.servico().build();
    }

    @Nested
    @DisplayName("Testes de CRUD")
    class CrudTests {

        @Test
        @DisplayName("deve listar serviços ativos")
        void deveListarServicosAtivos() {
            // Given
            ServicoResponseDTO response = responseDTO(servico);
            when(servicoRepository.findByAtivoTrue()).thenReturn(List.of(servico));
            when(servicoMapper.toResponse(servico)).thenReturn(response);

            // When
            List<ServicoResponseDTO> resultado = servicoService.listar();

            // Then
            assertThat(resultado).containsExactly(response);
        }

        @Test
        @DisplayName("deve buscar serviço por id")
        void deveBuscarServicoPorId() {
            // Given
            ServicoResponseDTO response = responseDTO(servico);
            when(servicoRepository.findById(servico.getId())).thenReturn(Optional.of(servico));
            when(servicoMapper.toResponse(servico)).thenReturn(response);

            // When
            ServicoResponseDTO resultado = servicoService.buscarPorId(servico.getId());

            // Then
            assertThat(resultado).isEqualTo(response);
        }

        @Test
        @DisplayName("deve lançar exceção ao buscar serviço inexistente")
        void deveLancarExcecaoAoBuscarServicoInexistente() {
            // Given
            UUID id = UUID.randomUUID();
            when(servicoRepository.findById(id)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> servicoService.buscarPorId(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Servico " + id + " não encontrado");
        }

        @Test
        @DisplayName("deve criar serviço")
        void deveCriarServico() {
            // Given
            ServicoCreateDTO dto = new ServicoCreateDTO("Balanceamento", "Rodas", new BigDecimal("120.00"), 45);
            Servico entidade = ServicoBuilder.servico().nome("Balanceamento").precoBase(new BigDecimal("120.00")).tempoEstimadoMinutos(45).build();
            ServicoResponseDTO response = responseDTO(servico);

            when(servicoMapper.toEntity(dto)).thenReturn(entidade);
            when(servicoRepository.save(entidade)).thenReturn(servico);
            when(servicoMapper.toResponse(servico)).thenReturn(response);

            // When
            ServicoResponseDTO resultado = servicoService.criar(dto);

            // Then
            assertThat(resultado).isEqualTo(response);
            verify(servicoRepository).save(entidade);
        }

        @Test
        @DisplayName("deve atualizar serviço")
        void deveAtualizarServico() {
            // Given
            ServicoCreateDTO dto = new ServicoCreateDTO("Alinhamento 3D", "Completo", new BigDecimal("220.00"), 90);
            ServicoResponseDTO response = responseDTO(servico);

            when(servicoRepository.findById(servico.getId())).thenReturn(Optional.of(servico));
            when(servicoRepository.save(servico)).thenReturn(servico);
            when(servicoMapper.toResponse(servico)).thenReturn(response);

            // When
            ServicoResponseDTO resultado = servicoService.atualizar(servico.getId(), dto);

            // Then
            assertThat(resultado).isEqualTo(response);
            assertThat(servico.getNome()).isEqualTo("Alinhamento 3D");
            assertThat(servico.getDescricao()).isEqualTo("Completo");
            assertThat(servico.getPrecoBase()).isEqualByComparingTo(new BigDecimal("220.00"));
            assertThat(servico.getTempoEstimadoMinutos()).isEqualTo(90);
        }

        @Test
        @DisplayName("deve lançar exceção ao atualizar serviço inexistente")
        void deveLancarExcecaoAoAtualizarServicoInexistente() {
            // Given
            UUID id = UUID.randomUUID();
            when(servicoRepository.findById(id)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> servicoService.atualizar(id, new ServicoCreateDTO("A", null, BigDecimal.ONE, 1)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Serviço não encontrado, ID: " + id);
        }

        @Test
        @DisplayName("deve desativar serviço")
        void deveDesativarServico() {
            // Given
            when(servicoRepository.findById(servico.getId())).thenReturn(Optional.of(servico));

            // When
            servicoService.desativar(servico.getId());

            // Then
            assertThat(servico.getAtivo()).isFalse();
            verify(servicoRepository).save(servico);
        }
    }

    private ServicoResponseDTO responseDTO(Servico servico) {
        return new ServicoResponseDTO(servico.getId(), servico.getNome(), servico.getDescricao(), servico.getPrecoBase(), servico.getTempoEstimadoMinutos(), servico.getAtivo());
    }
}

