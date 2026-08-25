package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.fake.GeradorMatriculaFixo;
import com.autopecas.autopecas.application.fake.TransacaoDireta;
import com.autopecas.autopecas.application.port.in.GestaoDeFuncionarios;
import com.autopecas.autopecas.application.port.in.view.FuncionarioView;
import com.autopecas.autopecas.application.port.out.FuncionarioRepositorio;
import com.autopecas.autopecas.domain.enums.TipoFuncionario;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.funcionario.Atendente;
import com.autopecas.autopecas.domain.model.funcionario.Funcionario;
import com.autopecas.autopecas.domain.model.funcionario.Mecanico;
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
 * Testes do caso de uso de funcionários.
 *
 * <p>A matrícula nunca vem do comando: ela é gerada pela port GeradorMatricula, e o prefixo
 * (MEC/ATD) é o que distingue os dois cadastros — por isso o gerador entra aqui como fake
 * determinístico em vez de mock.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GestaoDeFuncionariosUseCase")
class GestaoDeFuncionariosUseCaseTest {

    private static final UUID FUNCIONARIO_ID = UUID.randomUUID();
    private static final String CPF = "52998224725";

    @Mock
    private FuncionarioRepositorio funcionarioRepositorio;

    private GestaoDeFuncionarios gestao;

    @BeforeEach
    void setUp() {
        gestao = new GestaoDeFuncionariosUseCase(funcionarioRepositorio, new GeradorMatriculaFixo(),
                new TransacaoDireta());
    }

    private static GestaoDeFuncionarios.Cadastrar comando() {
        return new GestaoDeFuncionarios.Cadastrar("João", CPF, "joao@oficina.com", "11988887777",
                LocalDate.of(1985, 2, 10));
    }

    private static Mecanico mecanico() {
        return Mecanico.reconstituir(FUNCIONARIO_ID, "MEC-0001", CPF, "João", "joao@oficina.com",
                "11988887777", LocalDate.of(1985, 2, 10), true, null);
    }

    private static Atendente atendente() {
        return Atendente.reconstituir(FUNCIONARIO_ID, "ATD-0001", CPF, "Ana", "ana@oficina.com",
                "11977776666", LocalDate.of(1992, 8, 3), true, null);
    }

    @Nested
    @DisplayName("Cadastro")
    class Cadastro {

        @Test
        @DisplayName("mecânico deve receber matrícula com prefixo MEC e tipo MECANICO")
        void deveCadastrarMecanico() {
            when(funcionarioRepositorio.existePorCpf(CPF)).thenReturn(false);
            when(funcionarioRepositorio.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

            FuncionarioView view = gestao.cadastrarMecanico(comando());

            assertThat(view.matricula()).startsWith("MEC-");
            assertThat(view.tipo()).isEqualTo(TipoFuncionario.MECANICO.name());
        }

        @Test
        @DisplayName("atendente deve receber matrícula com prefixo ATD e tipo ATENDENTE")
        void deveCadastrarAtendente() {
            when(funcionarioRepositorio.existePorCpf(CPF)).thenReturn(false);
            when(funcionarioRepositorio.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

            FuncionarioView view = gestao.cadastrarAtendente(comando());

            assertThat(view.matricula()).startsWith("ATD-");
            assertThat(view.tipo()).isEqualTo(TipoFuncionario.ATENDENTE.name());
        }

        @Test
        @DisplayName("cada cadastro deve consumir uma matrícula nova")
        void matriculasNaoSeRepetem() {
            when(funcionarioRepositorio.existePorCpf(CPF)).thenReturn(false);
            when(funcionarioRepositorio.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

            String primeira = gestao.cadastrarMecanico(comando()).matricula();
            String segunda = gestao.cadastrarMecanico(comando()).matricula();

            assertThat(primeira).isNotEqualTo(segunda);
        }

        @Test
        @DisplayName("CPF duplicado deve ser recusado sem gravar nem consumir matrícula")
        void cpfDuplicadoEhRecusado() {
            when(funcionarioRepositorio.existePorCpf(CPF)).thenReturn(true);

            assertThatThrownBy(() -> gestao.cadastrarMecanico(comando()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já cadastrado");

            verify(funcionarioRepositorio, never()).salvar(any());
        }

        @Test
        @DisplayName("a checagem de CPF duplicado vale também para atendente")
        void cpfDuplicadoTambemNoAtendente() {
            when(funcionarioRepositorio.existePorCpf(CPF)).thenReturn(true);

            assertThatThrownBy(() -> gestao.cadastrarAtendente(comando()))
                    .isInstanceOf(BusinessException.class);

            verify(funcionarioRepositorio, never()).salvar(any());
        }
    }

    @Nested
    @DisplayName("Consultas e desativação")
    class ConsultasEDesativacao {

        @Test
        @DisplayName("deve listar ativos com o tipo de cada funcionário")
        void deveListarAtivos() {
            when(funcionarioRepositorio.ativos()).thenReturn(List.of(mecanico(), atendente()));

            List<FuncionarioView> views = gestao.listarAtivos();

            assertThat(views).extracting(FuncionarioView::tipo)
                    .containsExactly(TipoFuncionario.MECANICO.name(),
                            TipoFuncionario.ATENDENTE.name());
        }

        @Test
        @DisplayName("deve buscar por id")
        void deveBuscarPorId() {
            when(funcionarioRepositorio.porId(FUNCIONARIO_ID)).thenReturn(Optional.of(mecanico()));

            assertThat(gestao.porId(FUNCIONARIO_ID).matricula()).isEqualTo("MEC-0001");
        }

        @Test
        @DisplayName("id inexistente deve dar 404")
        void idInexistenteDaNotFound() {
            when(funcionarioRepositorio.porId(FUNCIONARIO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gestao.porId(FUNCIONARIO_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("desativar deve gravar o funcionário inativo")
        void desativarGravaInativo() {
            when(funcionarioRepositorio.porId(FUNCIONARIO_ID)).thenReturn(Optional.of(mecanico()));

            gestao.desativar(FUNCIONARIO_ID);

            ArgumentCaptor<Funcionario> capturado = ArgumentCaptor.forClass(Funcionario.class);
            verify(funcionarioRepositorio).salvar(capturado.capture());
            assertThat(capturado.getValue().isAtivo()).isFalse();
        }
    }
}
