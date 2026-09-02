package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.port.in.ControleDeAcessoDoCliente;
import com.autopecas.autopecas.application.port.out.ClienteRepositorio;
import com.autopecas.autopecas.application.port.out.OrdemServicoRepositorio;
import com.autopecas.autopecas.application.port.out.VeiculoRepositorio;
import com.autopecas.autopecas.domain.enums.Genero;
import com.autopecas.autopecas.domain.model.cliente.ClientePF;
import com.autopecas.autopecas.domain.model.os.OrdemServico;
import com.autopecas.autopecas.domain.model.veiculo.Veiculo;
import com.autopecas.autopecas.domain.vo.CPF;
import com.autopecas.autopecas.domain.vo.Placa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes da checagem de propriedade que sustenta a role CLIENTE.
 *
 * <p>São testes de segurança: cada caso negativo aqui corresponde a um vazamento de dados de um
 * cliente para outro. Por isso as asserções cobrem tanto o caminho feliz quanto todas as formas
 * de ausência — e-mail nulo, cliente sem cadastro, recurso inexistente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ControleDeAcessoDoClienteUseCase")
class ControleDeAcessoDoClienteUseCaseTest {

    private static final String EMAIL = "maria@test.com";
    private static final UUID CLIENTE_ID = UUID.randomUUID();
    private static final UUID OUTRO_CLIENTE_ID = UUID.randomUUID();
    private static final UUID VEICULO_ID = UUID.randomUUID();
    private static final UUID OS_ID = UUID.randomUUID();
    private static final String NUMERO_OS = "OS-000001";

    @Mock
    private ClienteRepositorio clienteRepositorio;
    @Mock
    private VeiculoRepositorio veiculoRepositorio;
    @Mock
    private OrdemServicoRepositorio ordemServicoRepositorio;

    private ControleDeAcessoDoCliente controle;

    @BeforeEach
    void setUp() {
        controle = new ControleDeAcessoDoClienteUseCase(clienteRepositorio, veiculoRepositorio,
                ordemServicoRepositorio);
    }

    private static ClientePF cliente() {
        return ClientePF.reconstituir(CLIENTE_ID, "Maria", EMAIL, "11999998888", true, true, null,
                new CPF("52998224725"), LocalDate.of(1990, 5, 20), null, Genero.FEMININO, null);
    }

    private static Veiculo veiculoDe(UUID clienteId) {
        return Veiculo.reconstituir(VEICULO_ID, new Placa("ABC1D23"), null, null, "VW", "Gol",
                2020, "Prata", null, true, clienteId);
    }

    private static OrdemServico osDe(UUID clienteId) {
        return OrdemServico.abrir(NUMERO_OS, clienteId, UUID.randomUUID(), "Barulho", null, null,
                null);
    }

    @Nested
    @DisplayName("Identidade do cliente")
    class Identidade {

        @Test
        @DisplayName("deve reconhecer o próprio cadastro")
        void reconheceOProprioCadastro() {
            when(clienteRepositorio.porEmail(EMAIL)).thenReturn(Optional.of(cliente()));

            assertThat(controle.ehOProprioCliente(EMAIL, CLIENTE_ID)).isTrue();
        }

        @Test
        @DisplayName("deve negar o cadastro de outro cliente")
        void negaCadastroDeOutro() {
            when(clienteRepositorio.porEmail(EMAIL)).thenReturn(Optional.of(cliente()));

            assertThat(controle.ehOProprioCliente(EMAIL, OUTRO_CLIENTE_ID)).isFalse();
        }

        @Test
        @DisplayName("e-mail sem cadastro correspondente deve negar")
        void emailSemCadastroNega() {
            when(clienteRepositorio.porEmail(EMAIL)).thenReturn(Optional.empty());

            assertThat(controle.ehOProprioCliente(EMAIL, CLIENTE_ID)).isFalse();
        }

        @Test
        @DisplayName("e-mail nulo ou em branco deve negar sem consultar o repositório")
        void emailAusenteNegaSemConsultar() {
            assertThat(controle.ehOProprioCliente(null, CLIENTE_ID)).isFalse();
            assertThat(controle.ehOProprioCliente("   ", CLIENTE_ID)).isFalse();

            verify(clienteRepositorio, never()).porEmail(any());
        }

        @Test
        @DisplayName("id nulo deve negar sem consultar o repositório")
        void idNuloNegaSemConsultar() {
            assertThat(controle.ehOProprioCliente(EMAIL, null)).isFalse();

            verify(clienteRepositorio, never()).porEmail(any());
        }
    }

    @Nested
    @DisplayName("Propriedade do veículo")
    class PropriedadeDoVeiculo {

        @Test
        @DisplayName("deve permitir veículo do próprio cliente")
        void permiteVeiculoProprio() {
            when(clienteRepositorio.porEmail(EMAIL)).thenReturn(Optional.of(cliente()));
            when(veiculoRepositorio.porId(VEICULO_ID)).thenReturn(Optional.of(veiculoDe(CLIENTE_ID)));

            assertThat(controle.ehDonoDoVeiculo(EMAIL, VEICULO_ID)).isTrue();
        }

        @Test
        @DisplayName("deve negar veículo de outro cliente")
        void negaVeiculoDeOutro() {
            when(clienteRepositorio.porEmail(EMAIL)).thenReturn(Optional.of(cliente()));
            when(veiculoRepositorio.porId(VEICULO_ID))
                    .thenReturn(Optional.of(veiculoDe(OUTRO_CLIENTE_ID)));

            assertThat(controle.ehDonoDoVeiculo(EMAIL, VEICULO_ID)).isFalse();
        }

        @Test
        @DisplayName("veículo inexistente deve negar em vez de lançar")
        void veiculoInexistenteNega() {
            when(clienteRepositorio.porEmail(EMAIL)).thenReturn(Optional.of(cliente()));
            when(veiculoRepositorio.porId(VEICULO_ID)).thenReturn(Optional.empty());

            assertThat(controle.ehDonoDoVeiculo(EMAIL, VEICULO_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("Propriedade da ordem de serviço")
    class PropriedadeDaOS {

        @Test
        @DisplayName("deve permitir OS do próprio cliente por id")
        void permiteOsPropriaPorId() {
            when(clienteRepositorio.porEmail(EMAIL)).thenReturn(Optional.of(cliente()));
            when(ordemServicoRepositorio.porId(OS_ID)).thenReturn(Optional.of(osDe(CLIENTE_ID)));

            assertThat(controle.ehDonoDaOrdemServico(EMAIL, OS_ID)).isTrue();
        }

        @Test
        @DisplayName("deve negar OS de outro cliente por id")
        void negaOsDeOutroPorId() {
            when(clienteRepositorio.porEmail(EMAIL)).thenReturn(Optional.of(cliente()));
            when(ordemServicoRepositorio.porId(OS_ID))
                    .thenReturn(Optional.of(osDe(OUTRO_CLIENTE_ID)));

            assertThat(controle.ehDonoDaOrdemServico(EMAIL, OS_ID)).isFalse();
        }

        @Test
        @DisplayName("deve permitir OS do próprio cliente por número")
        void permiteOsPropriaPorNumero() {
            when(clienteRepositorio.porEmail(EMAIL)).thenReturn(Optional.of(cliente()));
            when(ordemServicoRepositorio.porNumero(NUMERO_OS))
                    .thenReturn(Optional.of(osDe(CLIENTE_ID)));

            assertThat(controle.ehDonoDaOrdemServicoPorNumero(EMAIL, NUMERO_OS)).isTrue();
        }

        @Test
        @DisplayName("deve negar OS de outro cliente por número")
        void negaOsDeOutroPorNumero() {
            when(clienteRepositorio.porEmail(EMAIL)).thenReturn(Optional.of(cliente()));
            when(ordemServicoRepositorio.porNumero(NUMERO_OS))
                    .thenReturn(Optional.of(osDe(OUTRO_CLIENTE_ID)));

            assertThat(controle.ehDonoDaOrdemServicoPorNumero(EMAIL, NUMERO_OS)).isFalse();
        }

        @Test
        @DisplayName("número nulo ou em branco deve negar sem consultar")
        void numeroAusenteNegaSemConsultar() {
            assertThat(controle.ehDonoDaOrdemServicoPorNumero(EMAIL, null)).isFalse();
            assertThat(controle.ehDonoDaOrdemServicoPorNumero(EMAIL, "  ")).isFalse();

            verify(ordemServicoRepositorio, never()).porNumero(any());
        }

        @Test
        @DisplayName("OS inexistente deve negar em vez de lançar")
        void osInexistenteNega() {
            when(clienteRepositorio.porEmail(EMAIL)).thenReturn(Optional.of(cliente()));
            when(ordemServicoRepositorio.porId(OS_ID)).thenReturn(Optional.empty());

            assertThat(controle.ehDonoDaOrdemServico(EMAIL, OS_ID)).isFalse();
        }
    }
}
