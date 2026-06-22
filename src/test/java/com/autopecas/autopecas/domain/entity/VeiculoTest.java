package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.util.test.ClienteBuilder;
import com.autopecas.autopecas.util.test.VeiculoBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Veiculo")
class VeiculoTest {

    @Nested
    @DisplayName("Testes de Construtor e Builder")
    class ConstrutorBuilderTests {

        @Test
        @DisplayName("deve criar Veiculo com builder e valores padrão do builder")
        void deveCriarVeiculoComBuilderEValoresPadrao() {
            // Given
            Cliente cliente = ClienteBuilder.clientePF().build();
            Veiculo veiculo = VeiculoBuilder.veiculo(cliente).build();

            // Then
            assertThat(veiculo).isNotNull();
            assertThat(veiculo.getId()).isNotNull();
            assertThat(veiculo.getPlaca()).isEqualTo("ABC1B23");
            assertThat(veiculo.getChassi()).isEqualTo("1HGDM28153A000001");
            assertThat(veiculo.getRenavam()).isEqualTo("12345678901");
            assertThat(veiculo.getMarca()).isEqualTo("Chevrolet");
            assertThat(veiculo.getModelo()).isEqualTo("Onix");
            assertThat(veiculo.getAnoModelo()).isEqualTo(2020);
            assertThat(veiculo.getCor()).isEqualTo("Preto");
            assertThat(veiculo.getObservacoes()).isEqualTo("Veículo em bom estado");
            assertThat(veiculo.getAtivo()).isTrue();
            assertThat(veiculo.getCliente()).isEqualTo(cliente);
            assertThat(veiculo.getCreatedAt()).isNull(); // Gerado pela persistência
            assertThat(veiculo.getUpdatedAt()).isNull(); // Gerado pela persistência
            assertThat(veiculo.getOrdensServico()).isEmpty();
        }

        @Test
        @DisplayName("deve criar Veiculo com builder e todos os valores fornecidos")
        void deveCriarVeiculoComBuilderEValoresFornecidos() {
            // Given
            UUID id = UUID.randomUUID();
            String placa = "XYZ9E87";
            String chassi = "9FGHIJ01234KLMNO6";
            String renavam = "98765432109";
            String marca = "Volkswagen";
            String modelo = "Gol";
            Integer anoModelo = 2018;
            String cor = "Branco";
            String observacoes = "Pequenos arranhões na lateral";
            Boolean ativo = false;
            Cliente cliente = ClienteBuilder.clientePJ().build();

            // When
            Veiculo veiculo = Veiculo.builder()
                    .id(id)
                    .placa(placa)
                    .chassi(chassi)
                    .renavam(renavam)
                    .marca(marca)
                    .modelo(modelo)
                    .anoModelo(anoModelo)
                    .cor(cor)
                    .observacoes(observacoes)
                    .ativo(ativo)
                    .cliente(cliente)
                    .build();

            // Then
            assertThat(veiculo).isNotNull();
            assertThat(veiculo.getId()).isEqualTo(id);
            assertThat(veiculo.getPlaca()).isEqualTo(placa);
            assertThat(veiculo.getChassi()).isEqualTo(chassi);
            assertThat(veiculo.getRenavam()).isEqualTo(renavam);
            assertThat(veiculo.getMarca()).isEqualTo(marca);
            assertThat(veiculo.getModelo()).isEqualTo(modelo);
            assertThat(veiculo.getAnoModelo()).isEqualTo(anoModelo);
            assertThat(veiculo.getCor()).isEqualTo(cor);
            assertThat(veiculo.getObservacoes()).isEqualTo(observacoes);
            assertThat(veiculo.getAtivo()).isFalse();
            assertThat(veiculo.getCliente()).isEqualTo(cliente);
        }
    }

    @Nested
    @DisplayName("Testes de Equals e HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("deve retornar true para objetos Veiculo com o mesmo ID")
        void deveRetornarTrueParaObjetosVeiculoComMesmoID() {
            // Given
            UUID id = UUID.randomUUID();
            Cliente cliente = ClienteBuilder.clientePF().build();
            Veiculo veiculo1 = VeiculoBuilder.veiculo(cliente).id(id).placa("AAA1111").build();
            Veiculo veiculo2 = VeiculoBuilder.veiculo(cliente).id(id).placa("BBB2222").build(); // Placa diferente, mas ID igual

            // Then
            assertThat(veiculo1).isEqualTo(veiculo2);
            assertThat(veiculo1.hashCode()).isEqualTo(veiculo2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para objetos Veiculo com IDs diferentes")
        void deveRetornarFalseParaObjetosVeiculoComIDsDiferentes() {
            // Given
            Cliente cliente = ClienteBuilder.clientePF().build();
            Veiculo veiculo1 = VeiculoBuilder.veiculo(cliente).id(UUID.randomUUID()).placa("AAA1111").build();
            Veiculo veiculo2 = VeiculoBuilder.veiculo(cliente).id(UUID.randomUUID()).placa("AAA1111").build();

            // Then
            assertThat(veiculo1).isNotEqualTo(veiculo2);
            assertThat(veiculo1.hashCode()).isNotEqualTo(veiculo2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para Veiculo e objeto nulo")
        void deveRetornarFalseParaVeiculoEObjetoNulo() {
            // Given
            Cliente cliente = ClienteBuilder.clientePF().build();
            Veiculo veiculo = VeiculoBuilder.veiculo(cliente).build();

            // Then
            assertThat(veiculo).isNotEqualTo(null);
        }

        @Test
        @DisplayName("deve retornar false para Veiculo e objeto de classe diferente")
        void deveRetornarFalseParaVeiculoEObjetoDeClasseDiferente() {
            // Given
            Cliente cliente = ClienteBuilder.clientePF().build();
            Veiculo veiculo = VeiculoBuilder.veiculo(cliente).build();
            Object obj = new Object();

            // Then
            assertThat(veiculo).isNotEqualTo(obj);
        }
    }
}
