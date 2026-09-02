package com.autopecas.autopecas.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Endereco é um Value Object: imutável e comparado pelo conteúdo.
 *
 * <p>Os testes de setters e de construtor sem argumentos da versão anterior deixaram de existir
 * junto com o Embeddable mutável — um VO não tem setters, por definição.
 */
@DisplayName("Endereco")
class EnderecoTest {

    private static Endereco enderecoCompleto() {
        return new Endereco("01310-100", "Avenida Paulista", "1000", "Apto 42", "Bela Vista",
                "São Paulo", "SP");
    }

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("deve expor todos os campos informados")
        void deveExporTodosOsCampos() {
            Endereco endereco = enderecoCompleto();

            assertThat(endereco.cep()).isEqualTo("01310-100");
            assertThat(endereco.logradouro()).isEqualTo("Avenida Paulista");
            assertThat(endereco.numero()).isEqualTo("1000");
            assertThat(endereco.complemento()).isEqualTo("Apto 42");
            assertThat(endereco.bairro()).isEqualTo("Bela Vista");
            assertThat(endereco.cidade()).isEqualTo("São Paulo");
            assertThat(endereco.uf()).isEqualTo("SP");
        }

        @Test
        @DisplayName("deve aceitar campos opcionais nulos")
        void deveAceitarCamposOpcionaisNulos() {
            Endereco endereco = new Endereco("01310-100", null, null, null, null, "São Paulo", "SP");

            assertThat(endereco.cep()).isEqualTo("01310-100");
            assertThat(endereco.logradouro()).isNull();
            assertThat(endereco.complemento()).isNull();
        }
    }

    @Nested
    @DisplayName("Igualdade por valor")
    class IgualdadePorValor {

        @Test
        @DisplayName("endereços com o mesmo conteúdo devem ser iguais")
        void enderecosComMesmoConteudoDevemSerIguais() {
            assertThat(enderecoCompleto())
                    .isEqualTo(enderecoCompleto())
                    .hasSameHashCodeAs(enderecoCompleto());
        }

        @Test
        @DisplayName("endereços com campos diferentes não devem ser iguais")
        void enderecosDiferentesNaoDevemSerIguais() {
            Endereco paulista = new Endereco("01310-100", null, null, null, null, "São Paulo", "SP");
            Endereco carioca = new Endereco("20040-020", null, null, null, null, "Rio de Janeiro", "RJ");

            assertThat(paulista).isNotEqualTo(carioca);
        }
    }
}
