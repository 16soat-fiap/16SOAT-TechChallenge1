package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.TipoCliente;
import com.autopecas.autopecas.domain.valueobject.CNPJ;
import com.autopecas.autopecas.domain.valueobject.Endereco;
import com.autopecas.autopecas.util.test.ClienteBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClientePJ")
class ClientePJTest {

    @Nested
    @DisplayName("Testes de Construtor e Builder")
    class ConstrutorBuilderTests {

        @Test
        @DisplayName("deve criar ClientePJ com builder e valores padrão do builder")
        void deveCriarClientePJComBuilderEValoresPadrao() {
            // Given
            ClientePJ clientePJ = ClienteBuilder.clientePJ().build();

            // Then
            assertThat(clientePJ).isNotNull();
            assertThat(clientePJ.getId()).isNotNull();
            assertThat(clientePJ.getNome()).isEqualTo("Cliente PJ Teste");
            assertThat(clientePJ.getEmail()).isEqualTo("cliente.pj@teste.com");
            assertThat(clientePJ.getTelefone()).isEqualTo("11912345678");
            assertThat(clientePJ.getAceitaNotificacoes()).isTrue();
            assertThat(clientePJ.getAtivo()).isTrue();
            assertThat(clientePJ.getEndereco()).isNotNull();
            assertThat(clientePJ.getEndereco().getCep()).isEqualTo("02000-000");
            assertThat(clientePJ.getCnpj().getValor()).isEqualTo("12345678000195");
            assertThat(clientePJ.getRazaoSocial()).isEqualTo("Empresa Teste LTDA");
            assertThat(clientePJ.getInscricaoEstadual()).isEqualTo("ISENTA");
            assertThat(clientePJ.getInscricaoMunicipal()).isNull();
            assertThat(clientePJ.getContatoResponsavel()).isNull();
            assertThat(clientePJ.getCreatedAt()).isNull(); // Gerado pela persistência
            assertThat(clientePJ.getUpdatedAt()).isNull(); // Gerado pela persistência
            assertThat(clientePJ.getVeiculos()).isEmpty();
            assertThat(clientePJ.getOrdensServico()).isEmpty();
        }

        @Test
        @DisplayName("deve criar ClientePJ com builder e todos os valores fornecidos")
        void deveCriarClientePJComBuilderEValoresFornecidos() {
            // Given
            UUID id = UUID.randomUUID();
            String nome = "Nova Empresa";
            String email = "contato@novaempresa.com";
            String telefone = "31987654321";
            Boolean aceitaNotificacoes = false;
            Boolean ativo = false;
            Endereco endereco = Endereco.builder().cep("88888-888").logradouro("Av. Principal").build();
            CNPJ cnpj = new CNPJ("14830099000163");
            String razaoSocial = "Nova Empresa S.A.";
            String inscricaoEstadual = "123456789";
            String inscricaoMunicipal = "987654321";
            String contatoResponsavel = "João da Silva";

            // When
            ClientePJ clientePJ = ClientePJ.builder()
                    .id(id)
                    .nome(nome)
                    .email(email)
                    .telefone(telefone)
                    .aceitaNotificacoes(aceitaNotificacoes)
                    .ativo(ativo)
                    .endereco(endereco)
                    .cnpj(cnpj)
                    .razaoSocial(razaoSocial)
                    .inscricaoEstadual(inscricaoEstadual)
                    .inscricaoMunicipal(inscricaoMunicipal)
                    .contatoResponsavel(contatoResponsavel)
                    .build();

            // Then
            assertThat(clientePJ).isNotNull();
            assertThat(clientePJ.getId()).isEqualTo(id);
            assertThat(clientePJ.getNome()).isEqualTo(nome);
            assertThat(clientePJ.getEmail()).isEqualTo(email);
            assertThat(clientePJ.getTelefone()).isEqualTo(telefone);
            assertThat(clientePJ.getAceitaNotificacoes()).isFalse();
            assertThat(clientePJ.getAtivo()).isFalse();
            assertThat(clientePJ.getEndereco()).isEqualTo(endereco);
            assertThat(clientePJ.getCnpj().getValor()).isEqualTo(cnpj.getValor());
            assertThat(clientePJ.getRazaoSocial()).isEqualTo(razaoSocial);
            assertThat(clientePJ.getInscricaoEstadual()).isEqualTo(inscricaoEstadual);
            assertThat(clientePJ.getInscricaoMunicipal()).isEqualTo(inscricaoMunicipal);
            assertThat(clientePJ.getContatoResponsavel()).isEqualTo(contatoResponsavel);
        }
    }

    @Nested
    @DisplayName("Testes de Métodos Sobrescritos")
    class MetodosSobrescritosTests {

        @Test
        @DisplayName("getDocumento deve retornar o CNPJ")
        void getDocumentoDeveRetornarCNPJ() {
            // Given
            CNPJ cnpjEsperado = new CNPJ("01330542000195");
            ClientePJ clientePJ = ClienteBuilder.clientePJ().cnpj(cnpjEsperado).build();

            // When
            String documento = clientePJ.getDocumento();

            // Then
            assertThat(documento).isEqualTo(cnpjEsperado.getValor());
        }

        @Test
        @DisplayName("getTipo deve retornar TipoCliente.PJ")
        void getTipoDeveRetornarTipoClientePJ() {
            // Given
            ClientePJ clientePJ = ClienteBuilder.clientePJ().build();

            // When
            TipoCliente tipo = clientePJ.getTipo();

            // Then
            assertThat(tipo).isEqualTo(TipoCliente.PJ);
        }
    }

    @Nested
    @DisplayName("Testes de Equals e HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("deve retornar true para objetos ClientePJ com o mesmo ID")
        void deveRetornarTrueParaObjetosClientePJComMesmoID() {
            // Given
            UUID id = UUID.randomUUID();
            ClientePJ clientePJ1 = ClienteBuilder.clientePJ().id(id).cnpj(new CNPJ("24698442000111")).build();
            ClientePJ clientePJ2 = ClienteBuilder.clientePJ().id(id).cnpj(new CNPJ("58942500000122")).build(); // CNPJ diferente, mas ID igual

            // Then
            assertThat(clientePJ1).isEqualTo(clientePJ2);
            assertThat(clientePJ1.hashCode()).isEqualTo(clientePJ2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para objetos ClientePJ com IDs diferentes")
        void deveRetornarFalseParaObjetosClientePJComIDsDiferentes() {
            // Given
            ClientePJ clientePJ1 = ClienteBuilder.clientePJ().id(UUID.randomUUID()).cnpj(new CNPJ("24698442000111")).build();
            ClientePJ clientePJ2 = ClienteBuilder.clientePJ().id(UUID.randomUUID()).cnpj(new CNPJ("24698442000111")).build();

            // Then
            assertThat(clientePJ1).isNotEqualTo(clientePJ2);
            assertThat(clientePJ1.hashCode()).isNotEqualTo(clientePJ2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para ClientePJ e objeto nulo")
        void deveRetornarFalseParaClientePJEObjetoNulo() {
            // Given
            ClientePJ clientePJ = ClienteBuilder.clientePJ().build();

            // Then
            assertThat(clientePJ).isNotEqualTo(null);
        }

        @Test
        @DisplayName("deve retornar false para ClientePJ e objeto de classe diferente")
        void deveRetornarFalseParaClientePJEObjetoDeClasseDiferente() {
            // Given
            ClientePJ clientePJ = ClienteBuilder.clientePJ().build();
            Object obj = new Object();

            // Then
            assertThat(clientePJ).isNotEqualTo(obj);
        }
    }
}