package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.fake.TransacaoDireta;
import com.autopecas.autopecas.application.port.in.GestaoDeServicos;
import com.autopecas.autopecas.application.port.in.view.ServicoView;
import com.autopecas.autopecas.application.port.out.ServicoRepositorio;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.estoque.Servico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
 * Testes do caso de uso do catálogo de serviços.
 *
 * <p>O caso de uso é fino de propósito — a validação de nome e preço mora no agregado. Estes
 * testes verificam que ele delega, e não que reimplementa a regra.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GestaoDeServicosUseCase")
class GestaoDeServicosUseCaseTest {

    private static final UUID SERVICO_ID = UUID.randomUUID();

    @Mock
    private ServicoRepositorio servicoRepositorio;

    private GestaoDeServicos gestao;

    @BeforeEach
    void setUp() {
        gestao = new GestaoDeServicosUseCase(servicoRepositorio, new TransacaoDireta());
    }

    private static Servico servico() {
        return Servico.reconstituir(SERVICO_ID, "Troca de óleo", "Óleo e filtro",
                new BigDecimal("180.00"), 60, true);
    }

    @Nested
    @DisplayName("Cadastro")
    class Cadastro {

        @Test
        @DisplayName("deve cadastrar já ativo")
        void deveCadastrarAtivo() {
            when(servicoRepositorio.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

            ServicoView view = gestao.cadastrar(new GestaoDeServicos.DadosDoServico("Troca de óleo",
                    "Óleo e filtro", new BigDecimal("180.00"), 60));

            assertThat(view.ativo()).isTrue();
            assertThat(view.precoBase()).isEqualByComparingTo("180.00");
        }

        @Test
        @DisplayName("nome em branco é recusado pelo domínio, sem gravar")
        void nomeEmBrancoEhRecusado() {
            assertThatThrownBy(() -> gestao.cadastrar(new GestaoDeServicos.DadosDoServico("  ",
                    null, new BigDecimal("10.00"), null)))
                    .isInstanceOf(BusinessException.class);

            verify(servicoRepositorio, never()).salvar(any());
        }
    }

    @Nested
    @DisplayName("Atualização")
    class Atualizacao {

        @Test
        @DisplayName("deve aplicar nome e preço e gravar")
        void deveAtualizar() {
            Servico servico = servico();
            when(servicoRepositorio.porId(SERVICO_ID)).thenReturn(Optional.of(servico));
            when(servicoRepositorio.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

            ServicoView view = gestao.atualizar(SERVICO_ID, new GestaoDeServicos.DadosDoServico(
                    "Troca de óleo sintético", null, new BigDecimal("240.00"), null));

            assertThat(view.nome()).isEqualTo("Troca de óleo sintético");
            assertThat(view.precoBase()).isEqualByComparingTo("240.00");
        }

        @Test
        @DisplayName("descrição e tempo nulos não apagam os valores existentes")
        void camposOpcionaisNaoApagam() {
            Servico servico = servico();
            when(servicoRepositorio.porId(SERVICO_ID)).thenReturn(Optional.of(servico));
            when(servicoRepositorio.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

            ServicoView view = gestao.atualizar(SERVICO_ID, new GestaoDeServicos.DadosDoServico(
                    "Troca de óleo", null, new BigDecimal("180.00"), null));

            assertThat(view.descricao()).isEqualTo("Óleo e filtro");
            assertThat(view.tempoEstimadoMinutos()).isEqualTo(60);
        }

        @Test
        @DisplayName("serviço inexistente deve dar 404")
        void inexistenteDaNotFound() {
            when(servicoRepositorio.porId(SERVICO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gestao.atualizar(SERVICO_ID,
                    new GestaoDeServicos.DadosDoServico("X", null, BigDecimal.TEN, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Consultas e desativação")
    class ConsultasEDesativacao {

        @Test
        @DisplayName("deve listar os serviços ativos")
        void deveListarAtivos() {
            when(servicoRepositorio.ativos()).thenReturn(List.of(servico()));

            List<ServicoView> views = gestao.listarAtivos();

            assertThat(views).hasSize(1);
            assertThat(views.getFirst().nome()).isEqualTo("Troca de óleo");
        }

        @Test
        @DisplayName("deve buscar por id")
        void deveBuscarPorId() {
            when(servicoRepositorio.porId(SERVICO_ID)).thenReturn(Optional.of(servico()));

            assertThat(gestao.porId(SERVICO_ID).id()).isEqualTo(SERVICO_ID);
        }

        @Test
        @DisplayName("id inexistente deve dar 404")
        void idInexistenteDaNotFound() {
            when(servicoRepositorio.porId(SERVICO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gestao.porId(SERVICO_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("desativar deve gravar o serviço inativo")
        void desativarGravaInativo() {
            when(servicoRepositorio.porId(SERVICO_ID)).thenReturn(Optional.of(servico()));

            gestao.desativar(SERVICO_ID);

            ArgumentCaptor<Servico> capturado = ArgumentCaptor.forClass(Servico.class);
            verify(servicoRepositorio).salvar(capturado.capture());
            assertThat(capturado.getValue().isAtivo()).isFalse();
        }
    }
}
