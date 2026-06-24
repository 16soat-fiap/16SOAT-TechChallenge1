package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.Genero;
import com.autopecas.autopecas.domain.enums.TipoCliente;
import com.autopecas.autopecas.domain.valueobject.CPF;
import com.autopecas.autopecas.domain.valueobject.Endereco;
import com.autopecas.autopecas.util.test.ClienteBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClientePF")
class ClientePFTest {

    @Nested
    @DisplayName("Testes de Construtor e Builder")
    class ConstrutorBuilderTests {

        @Test
        @DisplayName("deve criar ClientePF com builder e valores padrão do builder")
        void deveCriarClientePFComBuilderEValoresPadrao() {
            // Given
            // Using the builder to get a default ClientePF instance
            ClientePF clientePF = ClienteBuilder.clientePF().build();

            // Then
            assertThat(clientePF).isNotNull();
            assertThat(clientePF.getId()).isNotNull();
            assertThat(clientePF.getNome()).isEqualTo("Cliente PF Teste");
            assertThat(clientePF.getEmail()).isEqualTo("cliente.pf@teste.com");
            assertThat(clientePF.getTelefone()).isEqualTo("11987654321");
            assertThat(clientePF.getAceitaNotificacoes()).isTrue();
            assertThat(clientePF.getAtivo()).isTrue();
            assertThat(clientePF.getEndereco()).isNotNull();
            assertThat(clientePF.getEndereco().getCep()).isEqualTo("01000-000");
            assertThat(clientePF.getCpf().getValor()).isEqualTo("52998224725");
            assertThat(clientePF.getDataNascimento()).isEqualTo(LocalDate.of(1990, 1, 1));
            assertThat(clientePF.getRg()).isEqualTo("123456789");
            assertThat(clientePF.getGenero()).isEqualTo(Genero.MASCULINO);
            assertThat(clientePF.getProfissao()).isEqualTo("Engenheiro");
            assertThat(clientePF.getCreatedAt()).isNull(); // Gerado pela persistência
            assertThat(clientePF.getUpdatedAt()).isNull(); // Gerado pela persistência
            assertThat(clientePF.getVeiculos()).isEmpty();
            assertThat(clientePF.getOrdensServico()).isEmpty();
        }

        @Test
        @DisplayName("deve criar ClientePF com builder e todos os valores fornecidos")
        void deveCriarClientePFComBuilderEValoresFornecidos() {
            // Given
            UUID id = UUID.randomUUID();
            String nome = "Novo Cliente PF";
            String email = "novo.cliente@email.com";
            String telefone = "21998877665";
            Boolean aceitaNotificacoes = false;
            Boolean ativo = false;
            Endereco endereco = Endereco.builder().cep("99999-999").logradouro("Rua Nova").build();
            CPF cpf = new CPF("23395216020");
            LocalDate dataNascimento = LocalDate.of(1985, 5, 10);
            String rg = "987654321";
            Genero genero = Genero.FEMININO;
            String profissao = "Arquiteta";

            // When
            ClientePF clientePF = ClientePF.builder()
                    .id(id)
                    .nome(nome)
                    .email(email)
                    .telefone(telefone)
                    .aceitaNotificacoes(aceitaNotificacoes)
                    .ativo(ativo)
                    .endereco(endereco)
                    .cpf(cpf)
                    .dataNascimento(dataNascimento)
                    .rg(rg)
                    .genero(genero)
                    .profissao(profissao)
                    .build();

            // Then
            assertThat(clientePF).isNotNull();
            assertThat(clientePF.getId()).isEqualTo(id);
            assertThat(clientePF.getNome()).isEqualTo(nome);
            assertThat(clientePF.getEmail()).isEqualTo(email);
            assertThat(clientePF.getTelefone()).isEqualTo(telefone);
            assertThat(clientePF.getAceitaNotificacoes()).isFalse();
            assertThat(clientePF.getAtivo()).isFalse();
            assertThat(clientePF.getEndereco()).isEqualTo(endereco);
            assertThat(clientePF.getCpf()).isEqualTo(cpf);
            assertThat(clientePF.getDataNascimento()).isEqualTo(dataNascimento);
            assertThat(clientePF.getRg()).isEqualTo(rg);
            assertThat(clientePF.getGenero()).isEqualTo(genero);
            assertThat(clientePF.getProfissao()).isEqualTo(profissao);
        }
    }

    @Nested
    @DisplayName("Testes de Métodos Sobrescritos")
    class MetodosSobrescritosTests {

        @Test
        @DisplayName("getDocumento deve retornar o CPF")
        void getDocumentoDeveRetornarCPF() {
            // Given
            CPF cpfEsperado = new CPF("06635258027");
            ClientePF clientePF = ClienteBuilder.clientePF().cpf(cpfEsperado).build();

            // When
            String documento = clientePF.getDocumento();

            // Then
            assertThat(documento).isEqualTo(cpfEsperado.getValor());
        }

        @Test
        @DisplayName("getTipo deve retornar TipoCliente.PF")
        void getTipoDeveRetornarTipoClientePF() {
            // Given
            ClientePF clientePF = ClienteBuilder.clientePF().build();

            // When
            TipoCliente tipo = clientePF.getTipo();

            // Then
            assertThat(tipo).isEqualTo(TipoCliente.PF);
        }
    }

    @Nested
    @DisplayName("Testes de Equals e HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("deve retornar true para objetos ClientePF com o mesmo ID")
        void deveRetornarTrueParaObjetosClientePFComMesmoID() {
            // Given
            UUID id = UUID.randomUUID();
            ClientePF clientePF1 = ClienteBuilder.clientePF().id(id).cpf(new CPF("78481006009")).build();
            ClientePF clientePF2 = ClienteBuilder.clientePF().id(id).cpf(new CPF("83488883060")).build(); // CPF diferente, mas ID igual

            // Then
            assertThat(clientePF1).isEqualTo(clientePF2);
            assertThat(clientePF1.hashCode()).isEqualTo(clientePF2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para objetos ClientePF com IDs diferentes")
        void deveRetornarFalseParaObjetosClientePFComIDsDiferentes() {
            // Given
            ClientePF clientePF1 = ClienteBuilder.clientePF().id(UUID.randomUUID()).cpf(new CPF("78481006009")).build();
            ClientePF clientePF2 = ClienteBuilder.clientePF().id(UUID.randomUUID()).cpf(new CPF("78481006009")).build();

            // Then
            assertThat(clientePF1).isNotEqualTo(clientePF2);
            assertThat(clientePF1.hashCode()).isNotEqualTo(clientePF2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para ClientePF e objeto nulo")
        void deveRetornarFalseParaClientePFEObjetoNulo() {
            // Given
            ClientePF clientePF = ClienteBuilder.clientePF().build();

            // Then
            assertThat(clientePF).isNotEqualTo(null);
        }

        @Test
        @DisplayName("deve retornar false para ClientePF e objeto de classe diferente")
        void deveRetornarFalseParaClientePFEObjetoDeClasseDiferente() {
            // Given
            ClientePF clientePF = ClienteBuilder.clientePF().build();
            Object obj = new Object();

            // Then
            assertThat(clientePF).isNotEqualTo(obj);
        }
    }
}