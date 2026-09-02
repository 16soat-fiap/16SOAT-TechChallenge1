package com.autopecas.autopecas.domain.vo;

import com.autopecas.autopecas.domain.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CPF")
class CPFTest {

    private static final String CPF_VALIDO_NUMERICO = "52998224725";
    private static final String CPF_VALIDO_FORMATADO = "529.982.247-25";

    @Nested
    @DisplayName("Criação e Validação")
    class CriacaoEValidacao {

        @Test
        @DisplayName("deve criar CPF com sucesso a partir de string numérica válida")
        void deveCriarCpfComStringNumerica() {
            CPF cpf = new CPF(CPF_VALIDO_NUMERICO);
            assertThat(cpf).isNotNull();
            assertThat(cpf.valor()).isEqualTo(CPF_VALIDO_NUMERICO);
        }

        @Test
        @DisplayName("deve criar CPF limpando a máscara se enviado formatado")
        void deveCriarCpfLimpandoMascara() {
            CPF cpf = new CPF(CPF_VALIDO_FORMATADO);
            assertThat(cpf.valor()).isEqualTo(CPF_VALIDO_NUMERICO);
        }

        @Test
        @DisplayName("deve estourar BusinessException para CPF com dígitos verificadores inválidos")
        void deveEstourarExceptionParaCpfInvalido() {
            assertThatThrownBy(() -> new CPF("11122233344"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CPF inválido");
        }

        @Test
        @DisplayName("deve estourar BusinessException para CPF com todos os dígitos iguais")
        void deveEstourarExceptionParaDigitosRepetidos() {
            assertThatThrownBy(() -> new CPF("11111111111"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CPF inválido");
        }

        @Test
        @DisplayName("deve estourar BusinessException para CPF com tamanho incorreto")
        void deveEstourarExceptionParaTamanhoIncorreto() {
            assertThatThrownBy(() -> new CPF("12345"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CPF inválido");
        }

        @Test
        @DisplayName("deve estourar IllegalArgumentException para entrada nula")
        void deveEstourarExceptionParaNulo() {
            assertThatThrownBy(() -> new CPF(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CPF não pode ser nulo");
        }

    }

    @Nested
    @DisplayName("Formatação")
    class Formatacao {

        @Test
        @DisplayName("deve retornar o CPF com os pontos e hífen corretos")
        void deveRetornarCpfFormatado() {
            CPF cpf = new CPF(CPF_VALIDO_NUMERICO);
            assertThat(cpf.formatado()).isEqualTo(CPF_VALIDO_FORMATADO);
        }
    }

    @Nested
    @DisplayName("Equals, HashCode e ToString")
    class EqualsHashCodeToString {

        @Test
        @DisplayName("dois CPFs com o mesmo valor devem ser considerados iguais")
        void doisCpfsIguais() {
            CPF cpf1 = new CPF(CPF_VALIDO_NUMERICO);
            CPF cpf2 = new CPF(CPF_VALIDO_FORMATADO);

            assertThat(cpf1).isEqualTo(cpf2);
            assertThat(cpf1.hashCode()).isEqualTo(cpf2.hashCode());
        }

        @Test
        @DisplayName("deve gerar String contendo o atributo do Lombok no toString")
        void deveTestarToString() {
            CPF cpf = new CPF(CPF_VALIDO_NUMERICO);
            assertThat(cpf.toString()).contains(CPF_VALIDO_NUMERICO);
        }
    }

    @Nested
    @DisplayName("Imutabilidade")
    class Imutabilidade {

        @Test
        @DisplayName("não deve expor nenhuma forma de alterar o valor após a criação")
        void naoDeveExporSetters() {
            CPF cpf = new CPF(CPF_VALIDO_NUMERICO);

            assertThat(CPF.class.getMethods())
                    .noneMatch(metodo -> metodo.getName().startsWith("set"));
            assertThat(cpf.valor()).isEqualTo(CPF_VALIDO_NUMERICO);
        }
    }
}