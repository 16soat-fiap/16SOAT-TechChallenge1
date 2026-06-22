package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.TipoFuncionario;
import com.autopecas.autopecas.domain.valueobject.Endereco;
import com.autopecas.autopecas.util.test.FuncionarioBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Atendente")
class AtendenteTest {

    @Nested
    @DisplayName("Testes de Construtor e Builder")
    class ConstrutorBuilderTests {

        @Test
        @DisplayName("deve criar Atendente com builder e valores padrão do builder")
        void deveCriarAtendenteComBuilderEValoresPadrao() {
            // Given
            Atendente atendente = FuncionarioBuilder.atendente().build();

            // Then
            assertThat(atendente).isNotNull();
            assertThat(atendente.getId()).isNotNull();
            assertThat(atendente.getMatricula()).isEqualTo("ATE001");
            assertThat(atendente.getCpf()).isEqualTo("55566677788");
            assertThat(atendente.getNome()).isEqualTo("Atendente Teste");
            assertThat(atendente.getEmail()).isEqualTo("atendente.teste@autopecas.com");
            assertThat(atendente.getTelefone()).isEqualTo("11977665544");
            assertThat(atendente.getDataNascimento()).isEqualTo(LocalDate.of(1992, 3, 20));
            assertThat(atendente.getAtivo()).isTrue();
            assertThat(atendente.getEndereco()).isNotNull();
            assertThat(atendente.getEndereco().getCep()).isEqualTo("04000-000");
            assertThat(atendente.getCreatedAt()).isNull(); // Gerado pela persistência
            assertThat(atendente.getUpdatedAt()).isNull(); // Gerado pela persistência
            assertThat(atendente.getOrdensRecepcionadas()).isEmpty();
            assertThat(atendente.getOrdensEntregues()).isEmpty();
        }

        @Test
        @DisplayName("deve criar Atendente com builder e todos os valores fornecidos")
        void deveCriarAtendenteComBuilderEValoresFornecidos() {
            // Given
            UUID id = UUID.randomUUID();
            String matricula = "ATE002";
            String cpf = "11100099988";
            String nome = "Nova Atendente";
            String email = "nova.atendente@autopecas.com";
            String telefone = "21998877665";
            LocalDate dataNascimento = LocalDate.of(1990, 7, 1);
            Boolean ativo = false;
            Endereco endereco = Endereco.builder().cep("06000-000").logradouro("Av. Secundária").build();

            // When
            Atendente atendente = Atendente.builder()
                    .id(id)
                    .matricula(matricula)
                    .cpf(cpf)
                    .nome(nome)
                    .email(email)
                    .telefone(telefone)
                    .dataNascimento(dataNascimento)
                    .ativo(ativo)
                    .endereco(endereco)
                    .build();

            // Then
            assertThat(atendente).isNotNull();
            assertThat(atendente.getId()).isEqualTo(id);
            assertThat(atendente.getMatricula()).isEqualTo(matricula);
            assertThat(atendente.getCpf()).isEqualTo(cpf);
            assertThat(atendente.getNome()).isEqualTo(nome);
            assertThat(atendente.getEmail()).isEqualTo(email);
            assertThat(atendente.getTelefone()).isEqualTo(telefone);
            assertThat(atendente.getDataNascimento()).isEqualTo(dataNascimento);
            assertThat(atendente.getAtivo()).isFalse();
            assertThat(atendente.getEndereco()).isEqualTo(endereco);
        }
    }

    @Nested
    @DisplayName("Testes de Métodos Sobrescritos e Herdados")
    class MetodosTests {

        @Test
        @DisplayName("getTipo deve retornar TipoFuncionario.ATENDENTE")
        void getTipoDeveRetornarTipoFuncionarioAtendente() {
            // Given
            Atendente atendente = FuncionarioBuilder.atendente().build();

            // When
            TipoFuncionario tipo = atendente.getTipo();

            // Then
            assertThat(tipo).isEqualTo(TipoFuncionario.ATENDENTE);
        }

        @Test
        @DisplayName("getIdentificacao deve retornar nome e matrícula formatados")
        void getIdentificacaoDeveRetornarNomeEMatriculaFormatados() {
            // Given
            String nome = "Atendente Teste";
            String matricula = "ATE001";
            Atendente atendente = FuncionarioBuilder.atendente()
                    .nome(nome)
                    .matricula(matricula)
                    .build();

            // When
            String identificacao = atendente.getIdentificacao();

            // Then
            assertThat(identificacao).isEqualTo("Atendente Teste (ATE001)");
        }
    }

    @Nested
    @DisplayName("Testes de Equals e HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("deve retornar true para objetos Atendente com o mesmo ID")
        void deveRetornarTrueParaObjetosAtendenteComMesmoID() {
            // Given
            UUID id = UUID.randomUUID();
            Atendente atendente1 = FuncionarioBuilder.atendente().id(id).matricula("ATE1").build();
            Atendente atendente2 = FuncionarioBuilder.atendente().id(id).matricula("ATE2").build(); // Matrícula diferente, mas ID igual

            // Then
            assertThat(atendente1).isEqualTo(atendente2);
            assertThat(atendente1.hashCode()).isEqualTo(atendente2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para objetos Atendente com IDs diferentes")
        void deveRetornarFalseParaObjetosAtendenteComIDsDiferentes() {
            // Given
            Atendente atendente1 = FuncionarioBuilder.atendente().id(UUID.randomUUID()).matricula("ATE1").build();
            Atendente atendente2 = FuncionarioBuilder.atendente().id(UUID.randomUUID()).matricula("ATE1").build();

            // Then
            assertThat(atendente1).isNotEqualTo(atendente2);
            assertThat(atendente1.hashCode()).isNotEqualTo(atendente2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para Atendente e objeto nulo")
        void deveRetornarFalseParaAtendenteEObjetoNulo() {
            // Given
            Atendente atendente = FuncionarioBuilder.atendente().build();

            // Then
            assertThat(atendente).isNotEqualTo(null);
        }

        @Test
        @DisplayName("deve retornar false para Atendente e objeto de classe diferente")
        void deveRetornarFalseParaAtendenteEObjetoDeClasseDiferente() {
            // Given
            Atendente atendente = FuncionarioBuilder.atendente().build();
            Object obj = new Object();

            // Then
            assertThat(atendente).isNotEqualTo(obj);
        }
    }
}
