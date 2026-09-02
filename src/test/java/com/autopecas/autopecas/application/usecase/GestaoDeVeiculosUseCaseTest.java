package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.fake.TransacaoDireta;
import com.autopecas.autopecas.application.port.in.GestaoDeVeiculos;
import com.autopecas.autopecas.application.port.in.view.VeiculoView;
import com.autopecas.autopecas.application.port.out.ClienteRepositorio;
import com.autopecas.autopecas.application.port.out.VeiculoRepositorio;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.veiculo.Veiculo;
import com.autopecas.autopecas.domain.vo.Placa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

/**
 * Testes do caso de uso de veículos.
 *
 * <p>Duas regras merecem destaque: só se opera sobre veículo ativo (a desativação é lógica) e a
 * unicidade de chassi/RENAVAM precisa ignorar o próprio veículo ao atualizar — senão nenhuma
 * atualização que reenvie os mesmos dados passaria.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GestaoDeVeiculosUseCase")
class GestaoDeVeiculosUseCaseTest {

    private static final UUID VEICULO_ID = UUID.randomUUID();
    private static final UUID CLIENTE_ID = UUID.randomUUID();
    private static final String CHASSI = "9BWZZZ377VT004251";
    private static final String RENAVAM = "12345678901";

    @Mock
    private VeiculoRepositorio veiculoRepositorio;
    @Mock
    private ClienteRepositorio clienteRepositorio;

    private GestaoDeVeiculos gestao;

    @BeforeEach
    void setUp() {
        gestao = new GestaoDeVeiculosUseCase(veiculoRepositorio, clienteRepositorio,
                new TransacaoDireta());
    }

    private static Veiculo veiculo() {
        return veiculoComId(VEICULO_ID);
    }

    private static Veiculo veiculoComId(UUID id) {
        return Veiculo.reconstituir(id, new Placa("ABC1D23"), CHASSI, RENAVAM, "VW", "Gol",
                2020, "Prata", null, true, CLIENTE_ID);
    }

    @Nested
    @DisplayName("Cadastro")
    class Cadastro {

        @Test
        @DisplayName("deve cadastrar vinculando ao cliente informado")
        void deveCadastrar() {
            when(clienteRepositorio.existePorId(CLIENTE_ID)).thenReturn(true);
            when(veiculoRepositorio.salvar(any())).thenReturn(veiculo());

            VeiculoView view = gestao.cadastrar(new GestaoDeVeiculos.Cadastrar(CLIENTE_ID,
                    "abc-1d23", CHASSI, RENAVAM, "VW", "Gol", 2020, "Prata"));

            assertThat(view.placa())
                    .as("a placa sai normalizada pelo Value Object")
                    .isEqualTo("ABC1D23");
            assertThat(view.clienteId()).isEqualTo(CLIENTE_ID);
        }

        @Test
        @DisplayName("deve recusar cliente inexistente sem gravar")
        void deveRecusarClienteInexistente() {
            when(clienteRepositorio.existePorId(CLIENTE_ID)).thenReturn(false);

            assertThatThrownBy(() -> gestao.cadastrar(new GestaoDeVeiculos.Cadastrar(CLIENTE_ID,
                    "ABC1D23", null, null, "VW", "Gol", 2020, "Prata")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Cliente não encontrado");

            verify(veiculoRepositorio, never()).salvar(any());
        }

        @Test
        @DisplayName("placa inválida falha antes de consultar o cliente")
        void placaInvalidaFalhaCedo() {
            assertThatThrownBy(() -> gestao.cadastrar(new GestaoDeVeiculos.Cadastrar(CLIENTE_ID,
                    "XX-999", null, null, "VW", "Gol", 2020, "Prata")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Placa inválida");

            verify(clienteRepositorio, never()).existePorId(any());
        }
    }

    @Nested
    @DisplayName("Atualização")
    class Atualizacao {

        @Test
        @DisplayName("o próprio veículo não conta como chassi duplicado")
        void proprioVeiculoNaoEhDuplicado() {
            Veiculo veiculo = veiculo();
            when(veiculoRepositorio.porIdAtivo(VEICULO_ID)).thenReturn(Optional.of(veiculo));
            when(veiculoRepositorio.porChassi(CHASSI)).thenReturn(Optional.of(veiculo));
            when(veiculoRepositorio.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

            VeiculoView view = gestao.atualizar(VEICULO_ID, new GestaoDeVeiculos.AtualizarDados(
                    null, "Gol G7", null, null, CHASSI, null));

            assertThat(view.modelo()).isEqualTo("Gol G7");
        }

        @Test
        @DisplayName("chassi de outro veículo deve ser recusado")
        void chassiDeOutroVeiculoEhRecusado() {
            when(veiculoRepositorio.porIdAtivo(VEICULO_ID)).thenReturn(Optional.of(veiculo()));
            when(veiculoRepositorio.porChassi(CHASSI))
                    .thenReturn(Optional.of(veiculoComId(UUID.randomUUID())));

            assertThatThrownBy(() -> gestao.atualizar(VEICULO_ID,
                    new GestaoDeVeiculos.AtualizarDados(null, null, null, null, CHASSI, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Chassi já cadastrado");

            verify(veiculoRepositorio, never()).salvar(any());
        }

        @Test
        @DisplayName("RENAVAM de outro veículo deve ser recusado")
        void renavamDeOutroVeiculoEhRecusado() {
            when(veiculoRepositorio.porIdAtivo(VEICULO_ID)).thenReturn(Optional.of(veiculo()));
            when(veiculoRepositorio.porRenavam(RENAVAM))
                    .thenReturn(Optional.of(veiculoComId(UUID.randomUUID())));

            assertThatThrownBy(() -> gestao.atualizar(VEICULO_ID,
                    new GestaoDeVeiculos.AtualizarDados(null, null, null, null, null, RENAVAM)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("RENAVAM já cadastrado");
        }

        @Test
        @DisplayName("campos nulos não disparam checagem de unicidade")
        void camposNulosNaoChecamUnicidade() {
            when(veiculoRepositorio.porIdAtivo(VEICULO_ID)).thenReturn(Optional.of(veiculo()));
            when(veiculoRepositorio.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

            gestao.atualizar(VEICULO_ID,
                    new GestaoDeVeiculos.AtualizarDados(null, null, null, "Preto", null, null));

            verify(veiculoRepositorio, never()).porChassi(any());
            verify(veiculoRepositorio, never()).porRenavam(any());
        }

        @Test
        @DisplayName("veículo inativo não pode ser atualizado")
        void inativoNaoAtualiza() {
            when(veiculoRepositorio.porIdAtivo(VEICULO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gestao.atualizar(VEICULO_ID,
                    new GestaoDeVeiculos.AtualizarDados(null, "X", null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("deve buscar por placa normalizando a entrada")
        void deveBuscarPorPlaca() {
            when(veiculoRepositorio.porPlacaAtiva(new Placa("ABC1D23")))
                    .thenReturn(Optional.of(veiculo()));

            assertThat(gestao.porPlaca(" abc-1d23 ").placa()).isEqualTo("ABC1D23");
        }

        @Test
        @DisplayName("placa sem veículo ativo deve dar 404")
        void placaSemVeiculoDaNotFound() {
            when(veiculoRepositorio.porPlacaAtiva(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gestao.porPlaca("ABC1D23"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("listar por cliente deve validar o cliente antes de consultar")
        void listarPorClienteValidaCliente() {
            when(clienteRepositorio.existePorId(CLIENTE_ID)).thenReturn(false);

            assertThatThrownBy(() -> gestao.doCliente(CLIENTE_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(veiculoRepositorio, never()).ativosDoCliente(any());
        }

        @Test
        @DisplayName("deve listar os veículos ativos do cliente")
        void deveListarDoCliente() {
            when(clienteRepositorio.existePorId(CLIENTE_ID)).thenReturn(true);
            when(veiculoRepositorio.ativosDoCliente(CLIENTE_ID)).thenReturn(List.of(veiculo()));

            List<VeiculoView> views = gestao.doCliente(CLIENTE_ID);

            assertThat(views).hasSize(1);
            assertThat(views.getFirst().clienteId()).isEqualTo(CLIENTE_ID);
        }
    }

    @Test
    @DisplayName("desativar deve gravar o veículo inativo")
    void desativarGravaInativo() {
        when(veiculoRepositorio.porIdAtivo(VEICULO_ID)).thenReturn(Optional.of(veiculo()));

        gestao.desativar(VEICULO_ID);

        ArgumentCaptor<Veiculo> capturado = ArgumentCaptor.forClass(Veiculo.class);
        verify(veiculoRepositorio).salvar(capturado.capture());
        assertThat(capturado.getValue().isAtivo()).isFalse();
    }
}
