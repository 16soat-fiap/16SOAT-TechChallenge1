package com.autopecas.autopecas.utils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
@DisplayName("DocumentValidator")
class DocumentValidatorTest {
    private static final String CPF_VALIDO_NUMERICO  = "52998224725";
    private static final String CPF_VALIDO_FORMATADO = "529.982.247-25";
    private static final String CNPJ_VALIDO_NUMERICO  = "11222333000181";
    private static final String CNPJ_VALIDO_FORMATADO = "11.222.333/0001-81";
    @Nested
    @DisplayName("validarCPF")
    class ValidarCPF {
        @Test
        @DisplayName("deve retornar true para CPF numérico válido")
        void deveRetornarTrueParaCpfNumericoValido() {
            assertThat(DocumentValidator.validarCPF(CPF_VALIDO_NUMERICO)).isTrue();
        }
        @Test
        @DisplayName("deve retornar true para CPF formatado com pontos e traço")
        void deveRetornarTrueParaCpfFormatado() {
            assertThat(DocumentValidator.validarCPF(CPF_VALIDO_FORMATADO)).isTrue();
        }
        @Test
        @DisplayName("deve retornar false para CPF com dígito verificador incorreto")
        void deveRetornarFalseParaCpfComDigitoErrado() {
            assertThat(DocumentValidator.validarCPF("52998224726")).isFalse();
        }
        @Test
        @DisplayName("deve retornar false para CPF com todos os dígitos iguais")
        void deveRetornarFalseParaCpfComDigitosRepetidos() {
            assertThat(DocumentValidator.validarCPF("11111111111")).isFalse();
            assertThat(DocumentValidator.validarCPF("00000000000")).isFalse();
        }
        @Test
        @DisplayName("deve retornar false para CPF com comprimento incorreto")
        void deveRetornarFalseParaCpfComComprimentoIncorreto() {
            assertThat(DocumentValidator.validarCPF("123456789")).isFalse();
            assertThat(DocumentValidator.validarCPF("529982247251")).isFalse();
        }
        @Test
        @DisplayName("deve retornar false para CPF nulo")
        void deveRetornarFalseParaCpfNulo() {
            assertThat(DocumentValidator.validarCPF(null)).isFalse();
        }
        @Test
        @DisplayName("deve retornar false para CPF vazio")
        void deveRetornarFalseParaCpfVazio() {
            assertThat(DocumentValidator.validarCPF("")).isFalse();
        }
    }
    @Nested
    @DisplayName("validarCNPJ")
    class ValidarCNPJ {
        @Test
        @DisplayName("deve retornar true para CNPJ numérico válido")
        void deveRetornarTrueParaCnpjNumericoValido() {
            assertThat(DocumentValidator.validarCNPJ(CNPJ_VALIDO_NUMERICO)).isTrue();
        }
        @Test
        @DisplayName("deve retornar true para CNPJ formatado")
        void deveRetornarTrueParaCnpjFormatado() {
            assertThat(DocumentValidator.validarCNPJ(CNPJ_VALIDO_FORMATADO)).isTrue();
        }
        @Test
        @DisplayName("deve retornar false para CNPJ com dígitos verificadores errados")
        void deveRetornarFalseParaCnpjComDigitosErrados() {
            assertThat(DocumentValidator.validarCNPJ("11222333000100")).isFalse();
        }
        @Test
        @DisplayName("deve retornar false para CNPJ com todos os dígitos iguais")
        void deveRetornarFalseParaCnpjComDigitosRepetidos() {
            assertThat(DocumentValidator.validarCNPJ("00000000000000")).isFalse();
        }
        @Test
        @DisplayName("deve retornar false para CNPJ nulo")
        void deveRetornarFalseParaCnpjNulo() {
            assertThat(DocumentValidator.validarCNPJ(null)).isFalse();
        }
        @Test
        @DisplayName("deve retornar false para CNPJ com comprimento incorreto")
        void deveRetornarFalseParaCnpjComComprimentoIncorreto() {
            assertThat(DocumentValidator.validarCNPJ("11222333")).isFalse();
        }
    }
}
