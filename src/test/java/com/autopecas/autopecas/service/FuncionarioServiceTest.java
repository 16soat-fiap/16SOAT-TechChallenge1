package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Atendente;
import com.autopecas.autopecas.domain.entity.Funcionario;
import com.autopecas.autopecas.domain.entity.Mecanico;
import com.autopecas.autopecas.dto.funcionario.AtendenteCreateDTO;
import com.autopecas.autopecas.dto.funcionario.FuncionarioResponseDTO;
import com.autopecas.autopecas.dto.funcionario.MecanicoCreateDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.FuncionarioMapper;
import com.autopecas.autopecas.repository.FuncionarioRepository;
import com.autopecas.autopecas.util.test.FuncionarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
@DisplayName("FuncionarioService")
class FuncionarioServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private FuncionarioMapper funcionarioMapper;

    @InjectMocks
    private FuncionarioService funcionarioService;

    private Mecanico mecanico;
    private Atendente atendente;

    @BeforeEach
    void setUp() {
        mecanico = FuncionarioBuilder.mecanico().build();
        atendente = FuncionarioBuilder.atendente().build();
    }

    @Nested
    @DisplayName("Testes de CRUD")
    class CrudTests {

        @Test
        @DisplayName("deve listar funcionários ativos")
        void deveListarFuncionariosAtivos() {
            // Given
            FuncionarioResponseDTO response = responseDTO(mecanico, "MECANICO");
            when(funcionarioRepository.findByAtivoTrue()).thenReturn(List.of(mecanico));
            when(funcionarioMapper.toResponse(mecanico)).thenReturn(response);

            // When
            List<FuncionarioResponseDTO> resultado = funcionarioService.listar();

            // Then
            assertThat(resultado).containsExactly(response);
        }

        @Test
        @DisplayName("deve buscar funcionário por id")
        void deveBuscarFuncionarioPorId() {
            // Given
            FuncionarioResponseDTO response = responseDTO(mecanico, "MECANICO");
            when(funcionarioRepository.findById(mecanico.getId())).thenReturn(Optional.of(mecanico));
            when(funcionarioMapper.toResponse(mecanico)).thenReturn(response);

            // When
            FuncionarioResponseDTO resultado = funcionarioService.buscarPorId(mecanico.getId());

            // Then
            assertThat(resultado).isEqualTo(response);
        }

        @Test
        @DisplayName("deve lançar exceção ao buscar funcionário inexistente")
        void deveLancarExcecaoAoBuscarFuncionarioInexistente() {
            // Given
            UUID id = UUID.randomUUID();
            when(funcionarioRepository.findById(id)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> funcionarioService.buscarPorId(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Funcionário não encontrado, id: " + id);
        }

        @Test
        @DisplayName("deve criar mecânico com matrícula sequencial")
        void deveCriarMecanicoComMatriculaSequencial() {
            // Given
            MecanicoCreateDTO dto = new MecanicoCreateDTO("Mecânico Novo", "12345678900", "mec@teste.com", "11999999999", LocalDate.of(1988, 2, 20));
            FuncionarioResponseDTO response = responseDTO(mecanico, "MECANICO");

            when(funcionarioRepository.existsByCpf(dto.cpf())).thenReturn(false);
            when(funcionarioRepository.proximoNumeroMecanico()).thenReturn(42L);
            when(funcionarioRepository.save(any(Mecanico.class))).thenReturn(mecanico);
            when(funcionarioMapper.toResponse(mecanico)).thenReturn(response);

            // When
            FuncionarioResponseDTO resultado = funcionarioService.criarMecanico(dto);

            // Then
            assertThat(resultado).isEqualTo(response);
            ArgumentCaptor<Mecanico> mecanicoCaptor = ArgumentCaptor.forClass(Mecanico.class);
            verify(funcionarioRepository).save(mecanicoCaptor.capture());
            assertThat(mecanicoCaptor.getValue().getMatricula()).isEqualTo("MEC-0042");
        }

        @Test
        @DisplayName("deve criar atendente com matrícula sequencial")
        void deveCriarAtendenteComMatriculaSequencial() {
            // Given
            AtendenteCreateDTO dto = new AtendenteCreateDTO("Atendente Novo", "12345678900", "atd@teste.com", "11888888888", LocalDate.of(1992, 7, 10));
            FuncionarioResponseDTO response = responseDTO(atendente, "ATENDENTE");

            when(funcionarioRepository.existsByCpf(dto.cpf())).thenReturn(false);
            when(funcionarioRepository.proximoNumeroAtendente()).thenReturn(7L);
            when(funcionarioRepository.save(any(Atendente.class))).thenReturn(atendente);
            when(funcionarioMapper.toResponse(atendente)).thenReturn(response);

            // When
            FuncionarioResponseDTO resultado = funcionarioService.criarAtendente(dto);

            // Then
            assertThat(resultado).isEqualTo(response);
            ArgumentCaptor<Atendente> atendenteCaptor = ArgumentCaptor.forClass(Atendente.class);
            verify(funcionarioRepository).save(atendenteCaptor.capture());
            assertThat(atendenteCaptor.getValue().getMatricula()).isEqualTo("ATD-0007");
        }

        @Test
        @DisplayName("deve bloquear criação quando CPF já existe")
        void deveBloquearCriacaoQuandoCpfJaExiste() {
            // Given
            MecanicoCreateDTO dto = new MecanicoCreateDTO("Nome", "12345678900", null, null, null);
            when(funcionarioRepository.existsByCpf(dto.cpf())).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> funcionarioService.criarMecanico(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("CPF: 12345678900 , já cadastrado");
        }

        @Test
        @DisplayName("deve desativar funcionário")
        void deveDesativarFuncionario() {
            // Given
            when(funcionarioRepository.findById(mecanico.getId())).thenReturn(Optional.of(mecanico));

            // When
            funcionarioService.desativar(mecanico.getId());

            // Then
            assertThat(mecanico.getAtivo()).isFalse();
            verify(funcionarioRepository).save(mecanico);
        }

        @Test
        @DisplayName("deve lançar exceção ao desativar funcionário inexistente")
        void deveLancarExcecaoAoDesativarFuncionarioInexistente() {
            // Given
            UUID id = UUID.randomUUID();
            when(funcionarioRepository.findById(id)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> funcionarioService.desativar(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Funcionário não encontrado com ID: " + id);

            verify(funcionarioRepository, never()).save(any(Funcionario.class));
        }
    }

    private FuncionarioResponseDTO responseDTO(Funcionario funcionario, String tipo) {
        return new FuncionarioResponseDTO(funcionario.getId(), funcionario.getMatricula(), funcionario.getNome(), funcionario.getEmail(), funcionario.getTelefone(), tipo, funcionario.getAtivo());
    }
}

