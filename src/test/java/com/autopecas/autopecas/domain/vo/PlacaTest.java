package com.autopecas.autopecas.domain.vo;

import com.autopecas.autopecas.domain.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Placa")
class PlacaTest {

    @Nested
    @DisplayName("Criação e Validação")
    class CriacaoEValidacao {

        @Test
        @DisplayName("deve criar placa válida no formato brasileiro antigo")
        void deveCriarPlacaFormatoAntigo() {
            Placa placa = new Placa("ABC1234");
            assertThat(placa).isNotNull();
            assertThat(placa.valor()).isEqualTo("ABC1234");
        }

        @Test
        @DisplayName("deve criar placa válida no formato Mercosul")
        void deveCriarPlacaFormatoMercosul() {
            Placa placa = new Placa("ABC1D23");
            assertThat(placa.valor()).isEqualTo("ABC1D23");
        }

        @Test
        @DisplayName("deve limpar hífens, espaços e converter letras minúsculas ao criar")
        void deveLimparEPadronizarEntrada() {
            Placa placa = new Placa(" abc-1d23 ");
            assertThat(placa.valor()).isEqualTo("ABC1D23");
        }

        @Test
        @DisplayName("deve estourar BusinessException para formato totalmente incorreto")
        void deveEstourarExceptionParaFormatoInvalido() {
            assertThatThrownBy(() -> new Placa("ABC-123XX"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Placa inválida");
        }

        @Test
        @DisplayName("deve estourar IllegalArgumentException para parâmetro nulo")
        void deveEstourarExceptionParaNulo() {
            assertThatThrownBy(() -> new Placa(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Placa não pode ser nula");
        }

    }

    @Nested
    @DisplayName("Formatação")
    class Formatacao {

        @Test
        @DisplayName("deve injetar o hífen visual separando as letras dos números")
        void deveRetornarPlacaComHifen() {
            Placa antiga = new Placa("ABC1234");
            Placa mercosul = new Placa("ABC1D23");

            assertThat(antiga.formatado()).isEqualTo("ABC-1234");
            assertThat(mercosul.formatado()).isEqualTo("ABC-1D23");
        }
    }

    @Nested
    @DisplayName("Equals, HashCode e ToString")
    class EqualsHashCodeToString {

        @Test
        @DisplayName("duas placas idênticas com escritas diferentes devem ser iguais")
        void duasPlacasIguais() {
            Placa p1 = new Placa("abc-1234");
            Placa p2 = new Placa("ABC1234");

            assertThat(p1).isEqualTo(p2);
            assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        }

        @Test
        @DisplayName("deve utilizar a saída estruturada pelo Lombok ToString")
        void deveTestarToString() {
            Placa placa = new Placa("ABC1234");
            assertThat(placa.toString()).contains("ABC1234");
        }
    }

    @Nested
    @DisplayName("Imutabilidade")
    class Imutabilidade {

        @Test
        @DisplayName("não deve expor nenhuma forma de alterar o valor após a criação")
        void naoDeveExporSetters() {
            Placa placa = new Placa("ABC1234");

            assertThat(Placa.class.getMethods())
                    .noneMatch(metodo -> metodo.getName().startsWith("set"));
            assertThat(placa.valor()).isEqualTo("ABC1234");
        }
    }
}