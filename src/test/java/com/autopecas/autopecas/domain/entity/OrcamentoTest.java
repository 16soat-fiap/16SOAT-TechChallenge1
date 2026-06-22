package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.StatusOrcamento;
import com.autopecas.autopecas.util.test.ClienteBuilder;
import com.autopecas.autopecas.util.test.FuncionarioBuilder;
import com.autopecas.autopecas.util.test.ItemOrcamentoPecaBuilder;
import com.autopecas.autopecas.util.test.ItemOrcamentoServicoBuilder;
import com.autopecas.autopecas.util.test.OrcamentoBuilder;
import com.autopecas.autopecas.util.test.OrdemServicoBuilder;
import com.autopecas.autopecas.util.test.PecaBuilder;
import com.autopecas.autopecas.util.test.ServicoBuilder;
import com.autopecas.autopecas.util.test.VeiculoBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Orcamento")
class OrcamentoTest {

    private OrdemServico ordemServico;
    private Atendente atendente;
    private Cliente cliente;
    private Veiculo veiculo;

    @BeforeEach
    void setUp() {
        cliente = ClienteBuilder.clientePF().build();
        veiculo = VeiculoBuilder.veiculo(cliente).build();
        atendente = FuncionarioBuilder.atendente().build();
        ordemServico = OrdemServicoBuilder.ordemServico(cliente, veiculo, atendente).build();
    }

    @Nested
    @DisplayName("Testes de Construtor e Builder")
    class ConstrutorBuilderTests {

        @Test
        @DisplayName("deve criar Orcamento com builder e valores padrão do builder")
        void deveCriarOrcamentoComBuilderEValoresPadrao() {
            // Given
            Orcamento orcamento = OrcamentoBuilder.orcamento(ordemServico).build();

            // Then
            assertThat(orcamento).isNotNull();
            assertThat(orcamento.getId()).isNotNull();
            assertThat(orcamento.getVersao()).isEqualTo(1);
            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.RASCUNHO);
            assertThat(orcamento.getValorMaoObra()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(orcamento.getValorPecas()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(orcamento.getValorAcrescimo()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(orcamento.getValorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(orcamento.getCondicoesPagamento()).isEqualTo("Pagamento em até 3x sem juros");
            assertThat(orcamento.getPrazoExecucaoDias()).isEqualTo(5);
            assertThat(orcamento.getDataValidade()).isNotNull();
            assertThat(orcamento.getObservacoes()).isEqualTo("Orçamento inicial");
            assertThat(orcamento.getOrdemServico()).isEqualTo(ordemServico);
            assertThat(orcamento.getElaboradoPor()).isNull();
            assertThat(orcamento.getItensServico()).isEmpty();
            assertThat(orcamento.getItensPeca()).isEmpty();
            assertThat(orcamento.getCreatedAt()).isNull(); // Gerado pela persistência
            assertThat(orcamento.getUpdatedAt()).isNull(); // Gerado pela persistência
        }

        @Test
        @DisplayName("deve criar Orcamento com builder e todos os valores fornecidos")
        void deveCriarOrcamentoComBuilderEValoresFornecidos() {
            // Given
            UUID id = UUID.randomUUID();
            Integer versao = 2;
            StatusOrcamento status = StatusOrcamento.APROVADA;
            BigDecimal valorMaoObra = new BigDecimal("100.00");
            BigDecimal valorPecas = new BigDecimal("200.00");
            BigDecimal valorAcrescimo = new BigDecimal("10.00");
            BigDecimal valorTotal = new BigDecimal("310.00");
            String condicoesPagamento = "À vista";
            Integer prazoExecucaoDias = 3;
            LocalDate dataValidade = LocalDate.now().plusDays(10);
            String observacoes = "Orçamento final";
            Atendente elaboradoPor = FuncionarioBuilder.atendente().build();

            // When
            Orcamento orcamento = Orcamento.builder()
                    .id(id)
                    .versao(versao)
                    .status(status)
                    .valorMaoObra(valorMaoObra)
                    .valorPecas(valorPecas)
                    .valorAcrescimo(valorAcrescimo)
                    .valorTotal(valorTotal)
                    .condicoesPagamento(condicoesPagamento)
                    .prazoExecucaoDias(prazoExecucaoDias)
                    .dataValidade(dataValidade)
                    .observacoes(observacoes)
                    .ordemServico(ordemServico)
                    .elaboradoPor(elaboradoPor)
                    .build();

            // Then
            assertThat(orcamento).isNotNull();
            assertThat(orcamento.getId()).isEqualTo(id);
            assertThat(orcamento.getVersao()).isEqualTo(versao);
            assertThat(orcamento.getStatus()).isEqualTo(status);
            assertThat(orcamento.getValorMaoObra()).isEqualByComparingTo(valorMaoObra);
            assertThat(orcamento.getValorPecas()).isEqualByComparingTo(valorPecas);
            assertThat(orcamento.getValorAcrescimo()).isEqualByComparingTo(valorAcrescimo);
            assertThat(orcamento.getValorTotal()).isEqualByComparingTo(valorTotal);
            assertThat(orcamento.getCondicoesPagamento()).isEqualTo(condicoesPagamento);
            assertThat(orcamento.getPrazoExecucaoDias()).isEqualTo(prazoExecucaoDias);
            assertThat(orcamento.getDataValidade()).isEqualTo(dataValidade);
            assertThat(orcamento.getObservacoes()).isEqualTo(observacoes);
            assertThat(orcamento.getOrdemServico()).isEqualTo(ordemServico);
            assertThat(orcamento.getElaboradoPor()).isEqualTo(elaboradoPor);
        }
    }

    @Nested
    @DisplayName("Testes de Métodos de Domínio")
    class MetodosDominioTests {

        private Orcamento orcamento;
        private Servico servico1;
        private Peca peca1;

        @BeforeEach
        void setupMetodos() {
            orcamento = OrcamentoBuilder.orcamento(ordemServico).build();
            servico1 = ServicoBuilder.servico().precoBase(new BigDecimal("50.00")).build();
            peca1 = PecaBuilder.peca().precoVenda(new BigDecimal("25.00")).build();
        }

        @Nested
        @DisplayName("Testes do método recalcular()")
        class RecalcularTests {

            @Test
            @DisplayName("deve recalcular valores com itens de serviço e peça e acréscimo")
            void deveRecalcularValoresComItensEServicoEPecaEAcrescimo() {
                // Given
                ItemOrcamentoServico itemServico1 = ItemOrcamentoServicoBuilder.itemOrcamentoServico(orcamento, servico1, 2, new BigDecimal("50.00"), null).build();
                ItemOrcamentoPeca itemPeca1 = ItemOrcamentoPecaBuilder.itemOrcamentoPeca(orcamento, peca1, 3, new BigDecimal("25.00")).build();

                orcamento.getItensServico().add(itemServico1);
                orcamento.getItensPeca().add(itemPeca1);
                orcamento.setValorAcrescimo(new BigDecimal("10.00"));

                // When
                orcamento.recalcular();

                // Then
                assertThat(orcamento.getValorMaoObra()).isEqualByComparingTo(new BigDecimal("100.00")); // 2 * 50.00
                assertThat(orcamento.getValorPecas()).isEqualByComparingTo(new BigDecimal("75.00")); // 3 * 25.00
                assertThat(orcamento.getValorTotal()).isEqualByComparingTo(new BigDecimal("185.00")); // 100 + 75 + 10
            }

            @Test
            @DisplayName("deve recalcular valores com listas vazias")
            void deveRecalcularValoresComListasVazias() {
                // Given
                orcamento.setValorAcrescimo(new BigDecimal("5.00"));

                // When
                orcamento.recalcular();

                // Then
                assertThat(orcamento.getValorMaoObra()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(orcamento.getValorPecas()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(orcamento.getValorTotal()).isEqualByComparingTo(new BigDecimal("5.00"));
            }

            @Test
            @DisplayName("deve recalcular valores com acréscimo zero")
            void deveRecalcularValoresComAcrescimoZero() {
                // Given
                ItemOrcamentoServico itemServico1 = ItemOrcamentoServicoBuilder.itemOrcamentoServico(orcamento, servico1, 1, new BigDecimal("50.00"), null).build();
                orcamento.getItensServico().add(itemServico1);
                orcamento.setValorAcrescimo(BigDecimal.ZERO);

                // When
                orcamento.recalcular();

                // Then
                assertThat(orcamento.getValorMaoObra()).isEqualByComparingTo(new BigDecimal("50.00"));
                assertThat(orcamento.getValorPecas()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(orcamento.getValorTotal()).isEqualByComparingTo(new BigDecimal("50.00"));
            }
        }

        @Nested
        @DisplayName("Testes do método enviar()")
        class EnviarTests {

            @Test
            @DisplayName("deve enviar orçamento com sucesso quando status é RASCUNHO")
            void deveEnviarOrcamentoComSucessoQuandoStatusRascunho() {
                // Given
                orcamento.setStatus(StatusOrcamento.RASCUNHO);

                // When
                orcamento.enviar();

                // Then
                assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.ENVIADA);
                assertThat(orcamento.getDataEnvio()).isNotNull();
                assertThat(orcamento.getDataEnvio()).isBeforeOrEqualTo(LocalDateTime.now());
            }

            @Test
            @DisplayName("nao deve enviar orçamento quando status nao é RASCUNHO")
            void naoDeveEnviarOrcamentoQuandoStatusNaoRascunho() {
                // Given
                orcamento.setStatus(StatusOrcamento.APROVADA); // Qualquer status diferente de RASCUNHO

                // When / Then
                assertThatThrownBy(() -> orcamento.enviar())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Apenas orçamentos em Rascunho podem ser enviados. Atual: " + StatusOrcamento.APROVADA);
                assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.APROVADA); // Status não deve mudar
                assertThat(orcamento.getDataEnvio()).isNull();
            }
        }

        @Nested
        @DisplayName("Testes do método aprovar()")
        class AprovarTests {

            @Test
            @DisplayName("deve aprovar orçamento com sucesso quando status é ENVIADA")
            void deveAprovarOrcamentoComSucessoQuandoStatusEnviada() {
                // Given
                orcamento.setStatus(StatusOrcamento.ENVIADA);

                // When
                orcamento.aprovar();

                // Then
                assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.APROVADA);
                assertThat(orcamento.getDataRespostaCliente()).isNotNull();
                assertThat(orcamento.getDataRespostaCliente()).isBeforeOrEqualTo(LocalDateTime.now());
            }

            @Test
            @DisplayName("nao deve aprovar orçamento quando status nao é ENVIADA")
            void naoDeveAprovarOrcamentoQuandoStatusNaoEnviada() {
                // Given
                orcamento.setStatus(StatusOrcamento.RASCUNHO); // Qualquer status diferente de ENVIADA

                // When / Then
                assertThatThrownBy(() -> orcamento.aprovar())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Apenas orçamentos enviados podem ser aprovados. Atual" + StatusOrcamento.RASCUNHO);
                assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.RASCUNHO); // Status não deve mudar
                assertThat(orcamento.getDataRespostaCliente()).isNull();
            }
        }

        @Nested
        @DisplayName("Testes do método rejeitar()")
        class RejeitarTests {

            private final String motivoRejeicao = "Preço muito alto";

            @Test
            @DisplayName("deve rejeitar orçamento com sucesso quando status é ENVIADA")
            void deveRejeitarOrcamentoComSucessoQuandoStatusEnviada() {
                // Given
                orcamento.setStatus(StatusOrcamento.ENVIADA);

                // When
                orcamento.rejeitar(motivoRejeicao);

                // Then
                assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.CANCELADA);
                assertThat(orcamento.getDataRespostaCliente()).isNotNull();
                assertThat(orcamento.getDataRespostaCliente()).isBeforeOrEqualTo(LocalDateTime.now());
                assertThat(orcamento.getMotivoRejeicao()).isEqualTo(motivoRejeicao);
            }

            @Test
            @DisplayName("nao deve rejeitar orçamento quando status nao é ENVIADA")
            void naoDeveRejeitarOrcamentoQuandoStatusNaoEnviada() {
                // Given
                orcamento.setStatus(StatusOrcamento.APROVADA); // Qualquer status diferente de ENVIADA

                // When / Then
                assertThatThrownBy(() -> orcamento.rejeitar(motivoRejeicao))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Apenas orçamentos enviados podem ser rejeitados. Atual:" + StatusOrcamento.APROVADA);
                assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.APROVADA); // Status não deve mudar
                assertThat(orcamento.getDataRespostaCliente()).isNull();
                assertThat(orcamento.getMotivoRejeicao()).isNull();
            }
        }

        @Nested
        @DisplayName("Testes dos métodos estaExpirado() e estaValido()")
        class ValidadeTests {

            @Test
            @DisplayName("estaExpirado deve retornar true quando dataValidade é anterior a hoje")
            void estaExpiradoDeveRetornarTrueQuandoDataValidadeAnteriorHoje() {
                // Given
                orcamento.setDataValidade(LocalDate.now().minusDays(1));

                // When
                boolean expirado = orcamento.estaExpirado();

                // Then
                assertThat(expirado).isTrue();
            }

            @Test
            @DisplayName("estaExpirado deve retornar false quando dataValidade é igual a hoje")
            void estaExpiradoDeveRetornarFalseQuandoDataValidadeIgualHoje() {
                // Given
                orcamento.setDataValidade(LocalDate.now());

                // When
                boolean expirado = orcamento.estaExpirado();

                // Then
                assertThat(expirado).isFalse();
            }

            @Test
            @DisplayName("estaExpirado deve retornar false quando dataValidade é posterior a hoje")
            void estaExpiradoDeveRetornarFalseQuandoDataValidadePosteriorHoje() {
                // Given
                orcamento.setDataValidade(LocalDate.now().plusDays(1));

                // When
                boolean expirado = orcamento.estaExpirado();

                // Then
                assertThat(expirado).isFalse();
            }

            @Test
            @DisplayName("estaExpirado deve retornar false quando dataValidade é nula")
            void estaExpiradoDeveRetornarFalseQuandoDataValidadeNula() {
                // Given
                orcamento.setDataValidade(null);

                // When
                boolean expirado = orcamento.estaExpirado();

                // Then
                assertThat(expirado).isFalse();
            }

            @Test
            @DisplayName("estaValido deve retornar true quando status é ENVIADA e não expirado")
            void estaValidoDeveRetornarTrueQuandoEnviadaENaoExpirado() {
                // Given
                orcamento.setStatus(StatusOrcamento.ENVIADA);
                orcamento.setDataValidade(LocalDate.now().plusDays(5));

                // When
                boolean valido = orcamento.estaValido();

                // Then
                assertThat(valido).isTrue();
            }

            @Test
            @DisplayName("estaValido deve retornar false quando status não é ENVIADA")
            void estaValidoDeveRetornarFalseQuandoStatusNaoEnviada() {
                // Given
                orcamento.setStatus(StatusOrcamento.RASCUNHO); // Não ENVIADA
                orcamento.setDataValidade(LocalDate.now().plusDays(5));

                // When
                boolean valido = orcamento.estaValido();

                // Then
                assertThat(valido).isFalse();
            }

            @Test
            @DisplayName("estaValido deve retornar false quando status é ENVIADA mas expirado")
            void estaValidoDeveRetornarFalseQuandoEnviadaMasExpirado() {
                // Given
                orcamento.setStatus(StatusOrcamento.ENVIADA);
                orcamento.setDataValidade(LocalDate.now().minusDays(1)); // Expirado

                // When
                boolean valido = orcamento.estaValido();

                // Then
                assertThat(valido).isFalse();
            }
        }

        @Nested
        @DisplayName("Testes do método expirar()")
        class ExpirarTests {

            @Test
            @DisplayName("deve mudar status para EXPIRADO quando ENVIADA e expirado")
            void deveMudarStatusParaExpiradoQuandoEnviadaEExpirado() {
                // Given
                orcamento.setStatus(StatusOrcamento.ENVIADA);
                orcamento.setDataValidade(LocalDate.now().minusDays(1));

                // When
                orcamento.expirar();

                // Then
                assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.EXPIRADO);
            }

            @Test
            @DisplayName("nao deve mudar status quando ENVIADA mas nao expirado")
            void naoDeveMudarStatusQuandoEnviadaMasNaoExpirado() {
                // Given
                orcamento.setStatus(StatusOrcamento.ENVIADA);
                orcamento.setDataValidade(LocalDate.now().plusDays(1));

                // When
                orcamento.expirar();

                // Then
                assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.ENVIADA); // Status não deve mudar
            }

            @Test
            @DisplayName("nao deve mudar status quando nao é ENVIADA")
            void naoDeveMudarStatusQuandoNaoEnviada() {
                // Given
                orcamento.setStatus(StatusOrcamento.RASCUNHO); // Não ENVIADA
                orcamento.setDataValidade(LocalDate.now().minusDays(1)); // Expirado

                // When
                orcamento.expirar();

                // Then
                assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.RASCUNHO); // Status não deve mudar
            }
        }
    }

    @Nested
    @DisplayName("Testes de Equals e HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("deve retornar true para objetos Orcamento com o mesmo ID")
        void deveRetornarTrueParaObjetosOrcamentoComMesmoID() {
            // Given
            UUID id = UUID.randomUUID();
            Orcamento orcamento1 = OrcamentoBuilder.orcamento(ordemServico).id(id).versao(1).build();
            Orcamento orcamento2 = OrcamentoBuilder.orcamento(ordemServico).id(id).versao(2).build(); // Versão diferente, mas ID igual

            // Then
            assertThat(orcamento1).isEqualTo(orcamento2);
            assertThat(orcamento1.hashCode()).isEqualTo(orcamento2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para objetos Orcamento com IDs diferentes")
        void deveRetornarFalseParaObjetosOrcamentoComIDsDiferentes() {
            // Given
            Orcamento orcamento1 = OrcamentoBuilder.orcamento(ordemServico).id(UUID.randomUUID()).build();
            Orcamento orcamento2 = OrcamentoBuilder.orcamento(ordemServico).id(UUID.randomUUID()).build();

            // Then
            assertThat(orcamento1).isNotEqualTo(orcamento2);
            assertThat(orcamento1.hashCode()).isNotEqualTo(orcamento2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para Orcamento e objeto nulo")
        void deveRetornarFalseParaOrcamentoEObjetoNulo() {
            // Given
            Orcamento orcamento = OrcamentoBuilder.orcamento(ordemServico).build();

            // Then
            assertThat(orcamento).isNotEqualTo(null);
        }

        @Test
        @DisplayName("deve retornar false para Orcamento e objeto de classe diferente")
        void deveRetornarFalseParaOrcamentoEObjetoDeClasseDiferente() {
            // Given
            Orcamento orcamento = OrcamentoBuilder.orcamento(ordemServico).build();
            Object obj = new Object();

            // Then
            assertThat(orcamento).isNotEqualTo(obj);
        }
    }
}
