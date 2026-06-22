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

@DisplayName("Mecanico")
class MecanicoTest {

    @Nested
    @DisplayName("Testes de Construtor e Builder")
    class ConstrutorBuilderTests {

        @Test
        @DisplayName("deve criar Mecanico com builder e valores padrão do builder")
        void deveCriarMecanicoComBuilderEValoresPadrao() {
            // Given
            Mecanico mecanico = FuncionarioBuilder.mecanico().build();

            // Then
            assertThat(mecanico).isNotNull();
            assertThat(mecanico.getId()).isNotNull();
            assertThat(mecanico.getMatricula()).isEqualTo("MEC001");
            assertThat(mecanico.getCpf()).isEqualTo("11122233344");
            assertThat(mecanico.getNome()).isEqualTo("Mecânico Teste");
            assertThat(mecanico.getEmail()).isEqualTo("mecanico.teste@autopecas.com");
            assertThat(mecanico.getTelefone()).isEqualTo("11998877665");
            assertThat(mecanico.getDataNascimento()).isEqualTo(LocalDate.of(1985, 10, 15));
            assertThat(mecanico.getAtivo()).isTrue();
            assertThat(mecanico.getEndereco()).isNotNull();
            assertThat(mecanico.getEndereco().getCep()).isEqualTo("03000-000");
            assertThat(mecanico.getCreatedAt()).isNull(); // Gerado pela persistência
            assertThat(mecanico.getUpdatedAt()).isNull(); // Gerado pela persistência
            assertThat(mecanico.getOrdensServico()).isEmpty();
        }

        @Test
        @DisplayName("deve criar Mecanico com builder e todos os valores fornecidos")
        void deveCriarMecanicoComBuilderEValoresFornecidos() {
            // Given
            UUID id = UUID.randomUUID();
            String matricula = "MEC002";
            String cpf = "99988877766";
            String nome = "Novo Mecânico";
            String email = "novo.mecanico@autopecas.com";
            String telefone = "21987654321";
            LocalDate dataNascimento = LocalDate.of(1995, 1, 20);
            Boolean ativo = false;
            Endereco endereco = Endereco.builder().cep("05000-000").logradouro("Rua Nova").build();

            // When
            Mecanico mecanico = Mecanico.builder()
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
            assertThat(mecanico).isNotNull();
            assertThat(mecanico.getId()).isEqualTo(id);
            assertThat(mecanico.getMatricula()).isEqualTo(matricula);
            assertThat(mecanico.getCpf()).isEqualTo(cpf);
            assertThat(mecanico.getNome()).isEqualTo(nome);
            assertThat(mecanico.getEmail()).isEqualTo(email);
            assertThat(mecanico.getTelefone()).isEqualTo(telefone);
            assertThat(mecanico.getDataNascimento()).isEqualTo(dataNascimento);
            assertThat(mecanico.getAtivo()).isFalse();
            assertThat(mecanico.getEndereco()).isEqualTo(endereco);
        }
    }

    @Nested
    @DisplayName("Testes de Métodos Sobrescritos e Herdados")
    class MetodosTests {

        @Test
        @DisplayName("getTipo deve retornar TipoFuncionario.MECANICO")
        void getTipoDeveRetornarTipoFuncionarioMecanico() {
            // Given
            Mecanico mecanico = FuncionarioBuilder.mecanico().build();

            // When
            TipoFuncionario tipo = mecanico.getTipo();

            // Then
            assertThat(tipo).isEqualTo(TipoFuncionario.MECANICO);
        }

        @Test
        @DisplayName("getIdentificacao deve retornar nome e matrícula formatados")
        void getIdentificacaoDeveRetornarNomeEMatriculaFormatados() {
            // Given
            String nome = "Mecânico Teste";
            String matricula = "MEC001";
            Mecanico mecanico = FuncionarioBuilder.mecanico()
                    .nome(nome)
                    .matricula(matricula)
                    .build();

            // When
            String identificacao = mecanico.getIdentificacao();

            // Then
            assertThat(identificacao).isEqualTo("Mecânico Teste (MEC001)");
        }
    }

    @Nested
    @DisplayName("Testes de Equals e HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("deve retornar true para objetos Mecanico com o mesmo ID")
        void deveRetornarTrueParaObjetosMecanicoComMesmoID() {
            // Given
            UUID id = UUID.randomUUID();
            Mecanico mecanico1 = FuncionarioBuilder.mecanico().id(id).matricula("MEC1").build();
            Mecanico mecanico2 = FuncionarioBuilder.mecanico().id(id).matricula("MEC2").build(); // Matrícula diferente, mas ID igual

            // Then
            assertThat(mecanico1).isEqualTo(mecanico2);
            assertThat(mecanico1.hashCode()).isEqualTo(mecanico2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para objetos Mecanico com IDs diferentes")
        void deveRetornarFalseParaObjetosMecanicoComIDsDiferentes() {
            // Given
            Mecanico mecanico1 = FuncionarioBuilder.mecanico().id(UUID.randomUUID()).matricula("MEC1").build();
            Mecanico mecanico2 = FuncionarioBuilder.mecanico().id(UUID.randomUUID()).matricula("MEC1").build();

            // Then
            assertThat(mecanico1).isNotEqualTo(mecanico2);
            assertThat(mecanico1.hashCode()).isNotEqualTo(mecanico2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para Mecanico e objeto nulo")
        void deveRetornarFalseParaMecanicoEObjetoNulo() {
            // Given
            Mecanico mecanico = FuncionarioBuilder.mecanico().build();

            // Then
            assertThat(mecanico).isNotEqualTo(null);
        }

        @Test
        @DisplayName("deve retornar false para Mecanico e objeto de classe diferente")
        void deveRetornarFalseParaMecanicoEObjetoDeClasseDiferente() {
            // Given
            Mecanico mecanico = FuncionarioBuilder.mecanico().build();
            Object obj = new Object();

            // Then
            assertThat(mecanico).isNotEqualTo(obj);
        }
    }
}
