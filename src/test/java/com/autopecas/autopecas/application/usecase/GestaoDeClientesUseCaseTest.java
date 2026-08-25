package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.fake.TransacaoDireta;
import com.autopecas.autopecas.application.port.in.GestaoDeClientes;
import com.autopecas.autopecas.application.port.in.view.ClienteView;
import com.autopecas.autopecas.application.port.out.ClienteRepositorio;
import com.autopecas.autopecas.domain.enums.Genero;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.cliente.Cliente;
import com.autopecas.autopecas.domain.model.cliente.ClientePF;
import com.autopecas.autopecas.domain.model.cliente.ClientePJ;
import com.autopecas.autopecas.domain.vo.CNPJ;
import com.autopecas.autopecas.domain.vo.CPF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

/**
 * Testes do caso de uso de clientes.
 *
 * <p>O que mais interessa aqui é a polimorfia PF/PJ resolvida na aplicação: o mesmo caso de uso
 * decide por tamanho do documento qual repositório consultar, e a view sai igual nos dois casos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GestaoDeClientesUseCase")
class GestaoDeClientesUseCaseTest {

    private static final String CPF_VALIDO = "52998224725";
    private static final String CNPJ_VALIDO = "00000000000191";
    private static final UUID CLIENTE_ID = UUID.randomUUID();

    @Mock
    private ClienteRepositorio clienteRepositorio;

    private GestaoDeClientes gestao;

    @BeforeEach
    void setUp() {
        gestao = new GestaoDeClientesUseCase(clienteRepositorio, new TransacaoDireta());
    }

    private static ClientePF clientePF() {
        return ClientePF.reconstituir(CLIENTE_ID, "Maria", "maria@test.com", "11999998888",
                true, true, null, new CPF(CPF_VALIDO), LocalDate.of(1990, 5, 20), "MG123",
                Genero.FEMININO, null);
    }

    private static ClientePJ clientePJ() {
        return ClientePJ.reconstituir(CLIENTE_ID, "Oficina X", "contato@x.com", "1133334444",
                true, true, null, new CNPJ(CNPJ_VALIDO), "Oficina X LTDA", "ISENTO", null, null);
    }

    @Nested
    @DisplayName("Cadastro de pessoa física")
    class CadastroPF {

        @Test
        @DisplayName("deve cadastrar e devolver o CPF normalizado como documento")
        void deveCadastrarPF() {
            when(clienteRepositorio.existePorCpf(any())).thenReturn(false);
            when(clienteRepositorio.salvar(any())).thenReturn(clientePF());

            ClienteView view = gestao.cadastrarPF(new GestaoDeClientes.CadastrarPF("Maria",
                    "maria@test.com", "11999998888", true, "529.982.247-25",
                    LocalDate.of(1990, 5, 20), "MG123", "FEMININO"));

            assertThat(view.documento())
                    .as("o CPF sai sem máscara, como o Value Object normaliza")
                    .isEqualTo(CPF_VALIDO);
            assertThat(view.nome()).isEqualTo("Maria");
        }

        @Test
        @DisplayName("deve recusar CPF já cadastrado sem tentar gravar")
        void deveRecusarCpfDuplicado() {
            when(clienteRepositorio.existePorCpf(any())).thenReturn(true);

            assertThatThrownBy(() -> gestao.cadastrarPF(new GestaoDeClientes.CadastrarPF("Maria",
                    null, null, true, CPF_VALIDO, null, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CPF já cadastrado");

            verify(clienteRepositorio, never()).salvar(any());
        }

        @Test
        @DisplayName("deve recusar CPF inválido antes de consultar o repositório")
        void deveRecusarCpfInvalido() {
            assertThatThrownBy(() -> gestao.cadastrarPF(new GestaoDeClientes.CadastrarPF("Maria",
                    null, null, true, "11122233344", null, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CPF inválido");

            verify(clienteRepositorio, never()).existePorCpf(any());
        }

        @Test
        @DisplayName("deve recusar gênero desconhecido")
        void deveRecusarGeneroInvalido() {
            assertThatThrownBy(() -> gestao.cadastrarPF(new GestaoDeClientes.CadastrarPF("Maria",
                    null, null, true, CPF_VALIDO, null, null, "INDEFINIDO")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Gênero inválido");
        }

        @Test
        @DisplayName("gênero nulo ou em branco é aceito e não vira erro")
        void generoOpcional() {
            when(clienteRepositorio.existePorCpf(any())).thenReturn(false);
            when(clienteRepositorio.salvar(any())).thenReturn(clientePF());

            gestao.cadastrarPF(new GestaoDeClientes.CadastrarPF("Maria", null, null, true,
                    CPF_VALIDO, null, null, "  "));

            verify(clienteRepositorio).salvar(any());
        }
    }

    @Nested
    @DisplayName("Cadastro de pessoa jurídica")
    class CadastroPJ {

        @Test
        @DisplayName("deve cadastrar e devolver o CNPJ como documento")
        void deveCadastrarPJ() {
            when(clienteRepositorio.existePorCnpj(any())).thenReturn(false);
            when(clienteRepositorio.salvar(any())).thenReturn(clientePJ());

            ClienteView view = gestao.cadastrarPJ(new GestaoDeClientes.CadastrarPJ("Oficina X",
                    "contato@x.com", "1133334444", true, "00.000.000/0001-91", "Oficina X LTDA",
                    "ISENTO"));

            assertThat(view.documento()).isEqualTo(CNPJ_VALIDO);
        }

        @Test
        @DisplayName("deve recusar CNPJ já cadastrado sem tentar gravar")
        void deveRecusarCnpjDuplicado() {
            when(clienteRepositorio.existePorCnpj(any())).thenReturn(true);

            assertThatThrownBy(() -> gestao.cadastrarPJ(new GestaoDeClientes.CadastrarPJ("Oficina X",
                    null, null, true, CNPJ_VALIDO, "Oficina X LTDA", null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CNPJ já cadastrado");

            verify(clienteRepositorio, never()).salvar(any());
        }
    }

    @Nested
    @DisplayName("Busca por documento")
    class BuscaPorDocumento {

        @Test
        @DisplayName("11 dígitos devem ir para a consulta de CPF")
        void eleveDigitosBuscamPorCpf() {
            when(clienteRepositorio.porCpf(any())).thenReturn(Optional.of(clientePF()));

            assertThat(gestao.porDocumento(CPF_VALIDO).documento()).isEqualTo(CPF_VALIDO);
            verify(clienteRepositorio, never()).porCnpj(any());
        }

        @Test
        @DisplayName("14 dígitos devem ir para a consulta de CNPJ")
        void quatorzeDigitosBuscamPorCnpj() {
            when(clienteRepositorio.porCnpj(any())).thenReturn(Optional.of(clientePJ()));

            assertThat(gestao.porDocumento(CNPJ_VALIDO).documento()).isEqualTo(CNPJ_VALIDO);
            verify(clienteRepositorio, never()).porCpf(any());
        }

        @Test
        @DisplayName("qualquer outro tamanho é erro de negócio, não 404")
        void tamanhoInvalidoEhErroDeNegocio() {
            assertThatThrownBy(() -> gestao.porDocumento("123"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Documento inválido");
        }

        @Test
        @DisplayName("documento válido sem cliente correspondente deve dar 404")
        void documentoSemClienteDaNotFound() {
            when(clienteRepositorio.porCpf(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gestao.porDocumento(CPF_VALIDO))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Consultas e alterações")
    class ConsultasEAlteracoes {

        @Test
        @DisplayName("deve listar apenas os clientes ativos")
        void deveListarAtivos() {
            when(clienteRepositorio.ativos()).thenReturn(List.of(clientePF(), clientePJ()));

            List<ClienteView> views = gestao.listarAtivos();

            assertThat(views).hasSize(2);
            assertThat(views).extracting(ClienteView::documento)
                    .containsExactly(CPF_VALIDO, CNPJ_VALIDO);
        }

        @Test
        @DisplayName("deve falhar ao buscar id inexistente")
        void deveFalharIdInexistente() {
            when(clienteRepositorio.porId(CLIENTE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gestao.porId(CLIENTE_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("atualização parcial deve preservar os campos não informados")
        void atualizacaoParcialPreservaCampos() {
            ClientePF cliente = clientePF();
            when(clienteRepositorio.porId(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(clienteRepositorio.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

            ClienteView view = gestao.atualizar(CLIENTE_ID,
                    new GestaoDeClientes.AtualizarDados("Maria Silva", null, null, null));

            assertThat(view.nome()).isEqualTo("Maria Silva");
            assertThat(view.email())
                    .as("e-mail nulo no comando não pode apagar o e-mail existente")
                    .isEqualTo("maria@test.com");
        }

        @Test
        @DisplayName("desativar deve gravar o cliente inativo")
        void desativarGravaInativo() {
            ClientePF cliente = clientePF();
            when(clienteRepositorio.porId(CLIENTE_ID)).thenReturn(Optional.of(cliente));

            gestao.desativar(CLIENTE_ID);

            ArgumentCaptor<Cliente> capturado = ArgumentCaptor.forClass(Cliente.class);
            verify(clienteRepositorio).salvar(capturado.capture());
            assertThat(capturado.getValue().isAtivo()).isFalse();
        }
    }
}
