package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.util.test.ServicoBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Servico")
class ServicoTest {

    @Nested
    @DisplayName("Testes de Construtor e Builder")
    class ConstrutorBuilderTests {

        @Test
        @DisplayName("deve criar Servico com builder e valores padrão do builder")
        void deveCriarServicoComBuilderEValoresPadrao() {
            // Given
            Servico servico = ServicoBuilder.servico().build();

            // Then
            assertThat(servico).isNotNull();
            assertThat(servico.getId()).isNotNull();
            assertThat(servico.getNome()).isEqualTo("Troca de Óleo");
            assertThat(servico.getDescricao()).isEqualTo("Troca de óleo do motor com filtro");
            assertThat(servico.getPrecoBase()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(servico.getTempoEstimadoMinutos()).isEqualTo(60);
            assertThat(servico.getAtivo()).isTrue();
            assertThat(servico.getCreatedAt()).isNull(); // Gerado pela persistência
            assertThat(servico.getUpdatedAt()).isNull(); // Gerado pela persistência
        }

        @Test
        @DisplayName("deve criar Servico com builder e todos os valores fornecidos")
        void deveCriarServicoComBuilderEValoresFornecidos() {
            // Given
            UUID id = UUID.randomUUID();
            String nome = "Alinhamento e Balanceamento";
            String descricao = "Alinhamento e balanceamento das 4 rodas";
            BigDecimal precoBase = new BigDecimal("200.00");
            Integer tempoEstimadoMinutos = 90;
            Boolean ativo = false;

            // When
            Servico servico = Servico.builder()
                    .id(id)
                    .nome(nome)
                    .descricao(descricao)
                    .precoBase(precoBase)
                    .tempoEstimadoMinutos(tempoEstimadoMinutos)
                    .ativo(ativo)
                    .build();

            // Then
            assertThat(servico).isNotNull();
            assertThat(servico.getId()).isEqualTo(id);
            assertThat(servico.getNome()).isEqualTo(nome);
            assertThat(servico.getDescricao()).isEqualTo(descricao);
            assertThat(servico.getPrecoBase()).isEqualByComparingTo(precoBase);
            assertThat(servico.getTempoEstimadoMinutos()).isEqualTo(tempoEstimadoMinutos);
            assertThat(servico.getAtivo()).isFalse();
        }
    }

    @Nested
    @DisplayName("Testes de Equals e HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("deve retornar true para objetos Servico com o mesmo ID")
        void deveRetornarTrueParaObjetosServicoComMesmoID() {
            // Given
            UUID id = UUID.randomUUID();
            Servico servico1 = ServicoBuilder.servico().id(id).nome("Servico A").build();
            Servico servico2 = ServicoBuilder.servico().id(id).nome("Servico B").build(); // Nome diferente, mas ID igual

            // Then
            assertThat(servico1).isEqualTo(servico2);
            assertThat(servico1.hashCode()).isEqualTo(servico2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para objetos Servico com IDs diferentes")
        void deveRetornarFalseParaObjetosServicoComIDsDiferentes() {
            // Given
            Servico servico1 = ServicoBuilder.servico().id(UUID.randomUUID()).nome("Servico A").build();
            Servico servico2 = ServicoBuilder.servico().id(UUID.randomUUID()).nome("Servico A").build();

            // Then
            assertThat(servico1).isNotEqualTo(servico2);
            assertThat(servico1.hashCode()).isNotEqualTo(servico2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para Servico e objeto nulo")
        void deveRetornarFalseParaServicoEObjetoNulo() {
            // Given
            Servico servico = ServicoBuilder.servico().build();

            // Then
            assertThat(servico).isNotEqualTo(null);
        }

        @Test
        @DisplayName("deve retornar false para Servico e objeto de classe diferente")
        void deveRetornarFalseParaServicoEObjetoDeClasseDiferente() {
            // Given
            Servico servico = ServicoBuilder.servico().build();
            Object obj = new Object();

            // Then
            assertThat(servico).isNotEqualTo(obj);
        }
    }
}
