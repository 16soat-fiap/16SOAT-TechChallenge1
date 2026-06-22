package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.domain.entity.Veiculo;
import com.autopecas.autopecas.dto.veiculo.VeiculoCreateDTO;
import com.autopecas.autopecas.dto.veiculo.VeiculoResponseDTO;
import com.autopecas.autopecas.dto.veiculo.VeiculoUpdateDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.VeiculoMapper;
import com.autopecas.autopecas.repository.ClienteRepository;
import com.autopecas.autopecas.repository.VeiculoRepository;
import com.autopecas.autopecas.util.test.ClienteBuilder;
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
@DisplayName("VeiculoService")
class VeiculoServiceTest {

    @Mock
    private VeiculoRepository veiculoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private VeiculoMapper veiculoMapper;

    @InjectMocks
    private VeiculoService veiculoService;

    private Cliente cliente;
    private Veiculo veiculo;

    @BeforeEach
    void setUp() {
        cliente = ClienteBuilder.clientePF().build();
        veiculo = VeiculoBuilder.veiculo(cliente).build();
    }

    @Nested
    @DisplayName("Testes de consulta")
    class ConsultaTests {

        @Test
        @DisplayName("deve listar veículos ativos")
        void deveListarVeiculosAtivos() {
            // Given
            VeiculoResponseDTO response = responseDTO(veiculo);
            when(veiculoRepository.findAllByAtivoTrue()).thenReturn(List.of(veiculo));
            when(veiculoMapper.toResponse(veiculo)).thenReturn(response);

            // When
            List<VeiculoResponseDTO> resultado = veiculoService.listar();

            // Then
            assertThat(resultado).containsExactly(response);
        }

        @Test
        @DisplayName("deve buscar veículo por id")
        void deveBuscarVeiculoPorId() {
            // Given
            VeiculoResponseDTO response = responseDTO(veiculo);
            when(veiculoRepository.findByIdAndAtivoTrue(veiculo.getId())).thenReturn(Optional.of(veiculo));
            when(veiculoMapper.toResponse(veiculo)).thenReturn(response);

            // When
            VeiculoResponseDTO resultado = veiculoService.buscarPorId(veiculo.getId());

            // Then
            assertThat(resultado).isEqualTo(response);
        }

        @Test
        @DisplayName("deve lançar exceção ao buscar veículo inexistente por id")
        void deveLancarExcecaoAoBuscarVeiculoInexistentePorId() {
            // Given
            UUID id = UUID.randomUUID();
            when(veiculoRepository.findByIdAndAtivoTrue(id)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> veiculoService.buscarPorId(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Veiculo não encontrado, id " + id);
        }

        @Test
        @DisplayName("deve buscar veículo por placa")
        void deveBuscarVeiculoPorPlaca() {
            // Given
            VeiculoResponseDTO response = responseDTO(veiculo);
            when(veiculoRepository.findByPlacaAndAtivoTrue(veiculo.getPlaca())).thenReturn(Optional.of(veiculo));
            when(veiculoMapper.toResponse(veiculo)).thenReturn(response);

            // When
            VeiculoResponseDTO resultado = veiculoService.buscarPorPlaca(veiculo.getPlaca().toLowerCase());

            // Then
            assertThat(resultado).isEqualTo(response);
        }

        @Test
        @DisplayName("deve listar veículos por cliente")
        void deveListarVeiculosPorCliente() {
            // Given
            VeiculoResponseDTO response = responseDTO(veiculo);
            when(clienteRepository.existsById(cliente.getId())).thenReturn(true);
            when(veiculoRepository.findByClienteIdAndAtivoTrue(cliente.getId())).thenReturn(List.of(veiculo));
            when(veiculoMapper.toResponse(veiculo)).thenReturn(response);

            // When
            List<VeiculoResponseDTO> resultado = veiculoService.listarPorCliente(cliente.getId());

            // Then
            assertThat(resultado).containsExactly(response);
        }

        @Test
        @DisplayName("deve lançar exceção quando o cliente não existe ao listar veículos")
        void deveLancarExcecaoQuandoOClienteNaoExisteAoListarVeiculos() {
            // Given
            UUID id = UUID.randomUUID();
            when(clienteRepository.existsById(id)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> veiculoService.listarPorCliente(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cliente não encontrado, id: " + id);
        }
    }

    @Nested
    @DisplayName("Testes de criação")
    class CriacaoTests {

        @Test
        @DisplayName("deve criar veículo")
        void deveCriarVeiculo() {
            // Given
            VeiculoCreateDTO dto = new VeiculoCreateDTO(cliente.getId(), "abc1b23", "1HGDM28153A000001", "12345678901", "GM", "Onix", 2024, "Branco");
            VeiculoResponseDTO response = responseDTO(veiculo);

            when(veiculoRepository.existsByPlaca("ABC1B23")).thenReturn(false);
            when(clienteRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
            when(veiculoRepository.save(any(Veiculo.class))).thenReturn(veiculo);
            when(veiculoMapper.toResponse(veiculo)).thenReturn(response);

            // When
            VeiculoResponseDTO resultado = veiculoService.criar(dto);

            // Then
            assertThat(resultado).isEqualTo(response);
            ArgumentCaptor<Veiculo> veiculoCaptor = ArgumentCaptor.forClass(Veiculo.class);
            verify(veiculoRepository).save(veiculoCaptor.capture());
            assertThat(veiculoCaptor.getValue().getPlaca()).isEqualTo("ABC1B23");
        }

        @Test
        @DisplayName("deve bloquear criação de veículo com placa duplicada")
        void deveBloquearCriacaoDeVeiculoComPlacaDuplicada() {
            // Given
            VeiculoCreateDTO dto = new VeiculoCreateDTO(cliente.getId(), "abc1b23", null, null, "GM", "Onix", 2024, null);
            when(veiculoRepository.existsByPlaca("ABC1B23")).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> veiculoService.criar(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Placa ABC1B23 já cadastrada");
        }
    }

    @Nested
    @DisplayName("Testes de atualização e exclusão lógica")
    class MutacaoTests {

        @Test
        @DisplayName("deve atualizar veículo")
        void deveAtualizarVeiculo() {
            // Given
            VeiculoUpdateDTO dto = new VeiculoUpdateDTO("Ford", "Ka", 2021, "Prata", "1HGDM28153A000009", "99999999999");
            VeiculoResponseDTO response = responseDTO(veiculo);

            when(veiculoRepository.findByIdAndAtivoTrue(veiculo.getId())).thenReturn(Optional.of(veiculo));
            when(veiculoRepository.findByChassi(dto.chassi())).thenReturn(Optional.empty());
            when(veiculoRepository.findByRenavam(dto.renavam())).thenReturn(Optional.empty());
            when(veiculoRepository.save(veiculo)).thenReturn(veiculo);
            when(veiculoMapper.toResponse(veiculo)).thenReturn(response);

            // When
            VeiculoResponseDTO resultado = veiculoService.atualizar(veiculo.getId(), dto);

            // Then
            assertThat(resultado).isEqualTo(response);
            assertThat(veiculo.getMarca()).isEqualTo("Ford");
            assertThat(veiculo.getModelo()).isEqualTo("Ka");
            assertThat(veiculo.getChassi()).isEqualTo("1HGDM28153A000009");
        }

        @Test
        @DisplayName("deve lançar exceção ao atualizar veículo inexistente")
        void deveLancarExcecaoAoAtualizarVeiculoInexistente() {
            // Given
            UUID id = UUID.randomUUID();
            when(veiculoRepository.findByIdAndAtivoTrue(id)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> veiculoService.atualizar(id, new VeiculoUpdateDTO("Ford", null, null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Veículo não encontrado, id: " + id);
        }

        @Test
        @DisplayName("deve bloquear atualização com chassi duplicado")
        void deveBloquearAtualizacaoComChassiDuplicado() {
            // Given
            Veiculo outroVeiculo = VeiculoBuilder.veiculo(cliente).id(UUID.randomUUID()).build();
            VeiculoUpdateDTO dto = new VeiculoUpdateDTO(null, null, null, null, outroVeiculo.getChassi(), null);

            when(veiculoRepository.findByIdAndAtivoTrue(veiculo.getId())).thenReturn(Optional.of(veiculo));
            when(veiculoRepository.findByChassi(outroVeiculo.getChassi())).thenReturn(Optional.of(outroVeiculo));

            // When / Then
            assertThatThrownBy(() -> veiculoService.atualizar(veiculo.getId(), dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Chassi já cadastrado.");
        }

        @Test
        @DisplayName("deve bloquear atualização com renavam duplicado")
        void deveBloquearAtualizacaoComRenavamDuplicado() {
            // Given
            Veiculo outroVeiculo = VeiculoBuilder.veiculo(cliente).id(UUID.randomUUID()).renavam("99999999999").build();
            VeiculoUpdateDTO dto = new VeiculoUpdateDTO(null, null, null, null, null, "99999999999");

            when(veiculoRepository.findByIdAndAtivoTrue(veiculo.getId())).thenReturn(Optional.of(veiculo));
            when(veiculoRepository.findByRenavam("99999999999")).thenReturn(Optional.of(outroVeiculo));

            // When / Then
            assertThatThrownBy(() -> veiculoService.atualizar(veiculo.getId(), dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("RENAVAM já cadastrado.");
        }

        @Test
        @DisplayName("deve desativar veículo")
        void deveDesativarVeiculo() {
            // Given
            when(veiculoRepository.findByIdAndAtivoTrue(veiculo.getId())).thenReturn(Optional.of(veiculo));

            // When
            veiculoService.deletar(veiculo.getId());

            // Then
            assertThat(veiculo.getAtivo()).isFalse();
            verify(veiculoRepository).save(veiculo);
        }

        @Test
        @DisplayName("deve lançar exceção ao desativar veículo inexistente")
        void deveLancarExcecaoAoDesativarVeiculoInexistente() {
            // Given
            UUID id = UUID.randomUUID();
            when(veiculoRepository.findByIdAndAtivoTrue(id)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> veiculoService.deletar(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Veículo não encontrado, id: " + id);

            verify(veiculoRepository, never()).save(any(Veiculo.class));
        }
    }

    private VeiculoResponseDTO responseDTO(Veiculo veiculo) {
        return new VeiculoResponseDTO(veiculo.getId(), veiculo.getPlaca(), veiculo.getMarca(), veiculo.getModelo(), veiculo.getAnoModelo(), veiculo.getCor(), veiculo.getAtivo(), cliente.getId());
    }
}

