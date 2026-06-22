package com.autopecas.autopecas.domain.valueobject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
@DisplayName("Endereco")
class EnderecoTest {
    @Nested
    @DisplayName("Criação via builder")
    class CriacaoViaBuilder {
        @Test
        @DisplayName("deve criar Endereco com todos os campos preenchidos")
        void deveCriarEnderecoComTodosOsCampos() {
            // Given / When
            Endereco endereco = Endereco.builder()
                    .cep("01310-100")
                    .logradouro("Avenida Paulista")
                    .numero("1000")
                    .complemento("Apto 42")
                    .bairro("Bela Vista")
                    .cidade("São Paulo")
                    .uf("SP")
                    .build();
            // Then
            assertThat(endereco).isNotNull();
            assertThat(endereco.getCep()).isEqualTo("01310-100");
            assertThat(endereco.getLogradouro()).isEqualTo("Avenida Paulista");
            assertThat(endereco.getNumero()).isEqualTo("1000");
            assertThat(endereco.getComplemento()).isEqualTo("Apto 42");
            assertThat(endereco.getBairro()).isEqualTo("Bela Vista");
            assertThat(endereco.getCidade()).isEqualTo("São Paulo");
            assertThat(endereco.getUf()).isEqualTo("SP");
        }
        @Test
        @DisplayName("deve criar Endereco vazio (sem validações no construtor)")
        void deveCriarEnderecoVazio() {
            Endereco endereco = new Endereco();
            assertThat(endereco).isNotNull();
            assertThat(endereco.getCep()).isNull();
            assertThat(endereco.getLogradouro()).isNull();
        }
    }
    @Nested
    @DisplayName("Equals e HashCode (Lombok @EqualsAndHashCode)")
    class EqualsHashCode {
        @Test
        @DisplayName("dois endereços com os mesmos campos devem ser iguais")
        void doisEnderecosComMesosCamposDevemSerIguais() {
            Endereco e1 = Endereco.builder().cep("01310-100").logradouro("Avenida Paulista").numero("1000").cidade("São Paulo").uf("SP").build();
            Endereco e2 = Endereco.builder().cep("01310-100").logradouro("Avenida Paulista").numero("1000").cidade("São Paulo").uf("SP").build();
            assertThat(e1).isEqualTo(e2);
            assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        }
        @Test
        @DisplayName("endereços com campos diferentes não devem ser iguais")
        void enderecosDiferentesNaoDevemSerIguais() {
            Endereco e1 = Endereco.builder().cep("01310-100").cidade("São Paulo").build();
            Endereco e2 = Endereco.builder().cep("20040-020").cidade("Rio de Janeiro").build();
            assertThat(e1).isNotEqualTo(e2);
        }
    }
    @Nested
    @DisplayName("Setters (Lombok @Setter)")
    class Setters {
        @Test
        @DisplayName("deve permitir alteração dos campos via setters")
        void devePermitirAlteracaoDeCampos() {
            Endereco endereco = new Endereco();
            endereco.setCep("01310-100");
            endereco.setCidade("São Paulo");
            assertThat(endereco.getCep()).isEqualTo("01310-100");
            assertThat(endereco.getCidade()).isEqualTo("São Paulo");
        }
    }
}
