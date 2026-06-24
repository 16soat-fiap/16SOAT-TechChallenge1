package com.autopecas.autopecas.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CNPJ")
class CNPJTest {

    private static final String CNPJ_VALIDO_NUMERICO = "00000000000191";
    private static final String CNPJ_VALIDO_FORMATADO = "00.000.000/0001-91";

    @Nested
    @DisplayName("Criação e Validação")
    class CriacaoEValidacao {

        @Test
        @DisplayName("deve criar CNPJ com sucesso a partir de string numérica válida")
        void deveCriarCnpjComStringNumerica() {
            CNPJ cnpj = new CNPJ(CNPJ_VALIDO_NUMERICO);
            assertThat(cnpj).isNotNull();
            assertThat(cnpj.getValor()).isEqualTo(CNPJ_VALIDO_NUMERICO);
        }

        @Test
        @DisplayName("deve criar CNPJ limpando os caracteres especiais")
        void deveCriarCnpjLimpandoCaracteres() {
            CNPJ cnpj = new CNPJ(CNPJ_VALIDO_FORMATADO);
            assertThat(cnpj.getValor()).isEqualTo(CNPJ_VALIDO_NUMERICO);
        }

        @Test
        @DisplayName("deve estourar IllegalArgumentException para CNPJ com cálculo inválido")
        void deveEstourarExceptionParaCnpjInvalido() {
            assertThatThrownBy(() -> new CNPJ("11222333000100"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CNPJ inválido");
        }

        @Test
        @DisplayName("deve estourar IllegalArgumentException para CNPJ com todos os dígitos idênticos")
        void deveEstourarExceptionParaDigitosRepetidos() {
            assertThatThrownBy(() -> new CNPJ("00000000000000"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CNPJ inválido");
        }

        @Test
        @DisplayName("deve estourar IllegalArgumentException para nulo")
        void deveEstourarExceptionParaNulo() {
            assertThatThrownBy(() -> new CNPJ(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CNPJ não pode ser nulo");
        }

        @Test
        @DisplayName("deve criar CNPJ vazio usando construtor do JPA")
        void deveCriarCnpjVazio() {
            CNPJ cnpj = new CNPJ();
            assertThat(cnpj).isNotNull();
            assertThat(cnpj.getValor()).isNull();
        }
    }

    @Nested
    @DisplayName("Formatação e Conversão")
    class FormatacaoEConversao {

        @Test
        @DisplayName("deve retornar CNPJ com máscara pelo método formatado")
        void deveRetornarFormatado() {
            CNPJ cnpj = new CNPJ(CNPJ_VALIDO_NUMERICO);
            assertThat(cnpj.formatado()).isEqualTo(CNPJ_VALIDO_FORMATADO);
        }

        @Test
        @DisplayName("deve retornar o valor formatado nativamente ao chamar toString")
        void deveRetornarFormatadoNoToString() {
            CNPJ cnpj = new CNPJ(CNPJ_VALIDO_NUMERICO);
            assertThat(cnpj.toString()).isEqualTo(CNPJ_VALIDO_FORMATADO);
        }
    }

    @Nested
    @DisplayName("Equals e HashCode")
    class EqualsHashCode {

        @Test
        @DisplayName("dois CNPJs com valores equivalentes devem ser iguais")
        void doisCnpjsIguais() {
            CNPJ cnpj1 = new CNPJ(CNPJ_VALIDO_NUMERICO);
            CNPJ cnpj2 = new CNPJ(CNPJ_VALIDO_FORMATADO);

            assertThat(cnpj1).isEqualTo(cnpj2);
            assertThat(cnpj1.hashCode()).isEqualTo(cnpj2.hashCode());
        }
    }
}