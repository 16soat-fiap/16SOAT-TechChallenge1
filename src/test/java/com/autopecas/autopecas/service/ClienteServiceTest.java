package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.domain.entity.ClientePF;
import com.autopecas.autopecas.domain.entity.ClientePJ;
import com.autopecas.autopecas.domain.valueobject.CNPJ;
import com.autopecas.autopecas.domain.valueobject.CPF;
import com.autopecas.autopecas.dto.cliente.ClienteCreatePFDTO;
import com.autopecas.autopecas.dto.cliente.ClienteCreatePJDTO;
import com.autopecas.autopecas.dto.cliente.ClienteResponseDTO;
import com.autopecas.autopecas.dto.cliente.ClienteUpdateDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.ClienteMapper;
import com.autopecas.autopecas.repository.ClientePFRepository;
import com.autopecas.autopecas.repository.ClientePJRepository;
import com.autopecas.autopecas.repository.ClienteRepository;
import com.autopecas.autopecas.util.test.ClienteBuilder;
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
@DisplayName("ClienteService")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ClientePFRepository clientePFRepository;
    @Mock
    private ClientePJRepository clientePJRepository;
    @Mock
    private ClienteMapper clienteMapper;

    @InjectMocks
    private ClienteService clienteService;

    private ClientePF clientePF;
    private ClientePJ clientePJ;

    @BeforeEach
    void setUp() {
        clientePF = ClienteBuilder.clientePF().build();
        clientePJ = ClienteBuilder.clientePJ().build();
    }

    @Nested
    @DisplayName("Testes de consulta")
    class ConsultaTests {

        @Test
        @DisplayName("deve listar clientes ativos")
        void deveListarClientesAtivos() {
            // Given
            ClienteResponseDTO response = responseDTO(clientePF);
            when(clienteRepository.findByAtivoTrue()).thenReturn(List.of(clientePF));
            when(clienteMapper.toResponse((Cliente) clientePF)).thenReturn(response);

            // When
            List<ClienteResponseDTO> resultado = clienteService.listar();

            // Then
            assertThat(resultado).containsExactly(response);
        }

        @Test
        @DisplayName("deve buscar cliente por id")
        void deveBuscarClientePorId() {
            // Given
            ClienteResponseDTO response = responseDTO(clientePF);
            when(clienteRepository.findById(clientePF.getId())).thenReturn(Optional.of(clientePF));
            when(clienteMapper.toResponse((Cliente) clientePF)).thenReturn(response);

            // When
            ClienteResponseDTO resultado = clienteService.buscarPorId(clientePF.getId());

            // Then
            assertThat(resultado).isEqualTo(response);
        }

        @Test
        @DisplayName("deve lançar exceção quando o cliente não existe ao buscar por id")
        void deveLancarExcecaoQuandoOClienteNaoExisteAoBuscarPorId() {
            // Given
            UUID id = UUID.randomUUID();
            when(clienteRepository.findById(id)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> clienteService.buscarPorId(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cliente não encontrado através do ID: " + id);
        }

        @Test
        @DisplayName("deve buscar cliente por CPF")
        void deveBuscarClientePorCpf() {
            // Given
            ClienteResponseDTO response = responseDTO(clientePF);
            when(clientePFRepository.findByCpf(clientePF.getCpf())).thenReturn(Optional.of(clientePF));
            when(clienteMapper.toResponse((Cliente) clientePF)).thenReturn(response);

            // When
            ClienteResponseDTO resultado = clienteService.buscarPorDocumento(clientePF.getCpf().getValor());

            // Then
            assertThat(resultado).isEqualTo(response);
        }

        @Test
        @DisplayName("deve buscar cliente por CNPJ")
        void deveBuscarClientePorCnpj() {
            // Given
            ClienteResponseDTO response = responseDTO(clientePJ);
//            when(clientePFRepository.findByCpf(clientePJ.getCnpj())).thenReturn(Optional.empty());
            when(clientePJRepository.findByCnpj(clientePJ.getCnpj())).thenReturn(Optional.of(clientePJ));
            when(clienteMapper.toResponse((Cliente) clientePJ)).thenReturn(response);

            // When
            ClienteResponseDTO resultado = clienteService.buscarPorDocumento(clientePJ.getDocumento());

            // Then
            assertThat(resultado).isEqualTo(response);
        }
    }

    @Nested
    @DisplayName("Testes de criação")
    class CriacaoTests {

        @Test
        @DisplayName("deve criar cliente PF")
        void deveCriarClientePf() {
            // Given
            ClienteCreatePFDTO dto = new ClienteCreatePFDTO(
                    "Cliente Novo",
                    "novo@teste.com",
                    "11999999999",
                    true,
                    "52998224725",
                    LocalDate.of(1990, 1, 1),
                    "RG123",
                    "MASCULINO"
            );
            ClienteResponseDTO response = new ClienteResponseDTO(clientePF.getId(), "Cliente Novo", dto.cpf(), dto.email(), dto.telefone());
            CPF cpf = new CPF(dto.cpf());

            when(clientePFRepository.existsByCpf(cpf)).thenReturn(false);
            when(clientePFRepository.save(any(ClientePF.class))).thenReturn(clientePF);
            when(clienteMapper.toResponse(clientePF)).thenReturn(response);

            // When
            ClienteResponseDTO resultado = clienteService.criarPF(dto);

            // Then
            assertThat(resultado).isEqualTo(response);
            ArgumentCaptor<ClientePF> clienteCaptor = ArgumentCaptor.forClass(ClientePF.class);
            verify(clientePFRepository).save(clienteCaptor.capture());
            assertThat(clienteCaptor.getValue().getNome()).isEqualTo("Cliente Novo");
            assertThat(clienteCaptor.getValue().getCpf().getValor()).isEqualTo("52998224725");
        }

        @Test
        @DisplayName("deve bloquear criação de cliente PF com CPF duplicado")
        void deveBloquearCriacaoDeClientePfComCpfDuplicado() {
            // Given
            ClienteCreatePFDTO dto = new ClienteCreatePFDTO("Cliente", null, null, true, "52998224725", null, null, null);
            when(clientePFRepository.existsByCpf(new CPF(dto.cpf()))).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> clienteService.criarPF(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("CPF já cadastrado: 52998224725");
        }

        @Test
        @DisplayName("deve criar cliente PJ")
        void deveCriarClientePj() {
            // Given
            ClienteCreatePJDTO dto = new ClienteCreatePJDTO(
                    "Empresa Nova",
                    "empresa@teste.com",
                    "1133333333",
                    true,
                    "12345678000195",
                    "Empresa Nova LTDA",
                    "12345"
            );
            ClienteResponseDTO response = new ClienteResponseDTO(clientePJ.getId(), dto.nome(), dto.cnpj(), dto.email(), dto.telefone());
            CNPJ cnpj = new CNPJ(dto.cnpj());

            when(clientePJRepository.existsByCnpj(cnpj)).thenReturn(false);
            when(clientePJRepository.save(any(ClientePJ.class))).thenReturn(clientePJ);
            when(clienteMapper.toResponse(clientePJ)).thenReturn(response);

            // When
            ClienteResponseDTO resultado = clienteService.criarPJ(dto);

            // Then
            assertThat(resultado).isEqualTo(response);
        }

        @Test
        @DisplayName("deve bloquear criação de cliente PJ com CNPJ duplicado")
        void deveBloquearCriacaoDeClientePjComCnpjDuplicado() {
            // Given
            ClienteCreatePJDTO dto = new ClienteCreatePJDTO("Empresa", null, null, true, "12345678000195", "Empresa", null);
            CNPJ cnpj = new CNPJ(dto.cnpj());

            when(clientePJRepository.existsByCnpj(cnpj)).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> clienteService.criarPJ(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("CNPJ já cadastrado: 12345678000195");
        }
    }

    @Nested
    @DisplayName("Testes de atualização e exclusão lógica")
    class MutacaoTests {

        @Test
        @DisplayName("deve atualizar cliente")
        void deveAtualizarCliente() {
            // Given
            ClienteUpdateDTO dto = new ClienteUpdateDTO("Nome Atualizado", "atualizado@teste.com", "11888888888", false);
            ClienteResponseDTO response = new ClienteResponseDTO(clientePF.getId(), dto.nome(), clientePF.getCpf().toString(), dto.email(), dto.telefone());

            when(clienteRepository.findById(clientePF.getId())).thenReturn(Optional.of(clientePF));
            when(clienteRepository.save(clientePF)).thenReturn(clientePF);
            when(clienteMapper.toResponse((Cliente) clientePF)).thenReturn(response);

            // When
            ClienteResponseDTO resultado = clienteService.atualizar(clientePF.getId(), dto);

            // Then
            assertThat(resultado).isEqualTo(response);
            assertThat(clientePF.getNome()).isEqualTo("Nome Atualizado");
            assertThat(clientePF.getEmail()).isEqualTo("atualizado@teste.com");
            assertThat(clientePF.getAceitaNotificacoes()).isFalse();
        }

        @Test
        @DisplayName("deve lançar exceção ao atualizar cliente inexistente")
        void deveLancarExcecaoAoAtualizarClienteInexistente() {
            // Given
            UUID id = UUID.randomUUID();
            when(clienteRepository.findById(id)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> clienteService.atualizar(id, new ClienteUpdateDTO("Nome", null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cliente não encontrado, id: " + id);
        }

        @Test
        @DisplayName("deve desativar cliente")
        void deveDesativarCliente() {
            // Given
            when(clienteRepository.findById(clientePF.getId())).thenReturn(Optional.of(clientePF));

            // When
            clienteService.desativar(clientePF.getId());

            // Then
            assertThat(clientePF.getAtivo()).isFalse();
            verify(clienteRepository).save(clientePF);
        }

        @Test
        @DisplayName("deve lançar exceção ao desativar cliente inexistente")
        void deveLancarExcecaoAoDesativarClienteInexistente() {
            // Given
            UUID id = UUID.randomUUID();
            when(clienteRepository.findById(id)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> clienteService.desativar(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cliente não encontrado com ID: " + id);

            verify(clienteRepository, never()).save(any(Cliente.class));
        }
    }

    private ClienteResponseDTO responseDTO(Cliente cliente) {

        return new ClienteResponseDTO(cliente.getId(), cliente.getNome(), cliente.getDocumento(), cliente.getEmail(), cliente.getTelefone());
    }
}