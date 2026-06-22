package com.autopecas.autopecas.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Peca")
class PecaTest {

    @Nested
    @DisplayName("Testes de Construtor e Builder")
    class ConstrutorBuilderTests {

        @Test
        @DisplayName("deve criar Peca com builder e valores padrão")
        void deveCriarPecaComBuilderEValoresPadrao() {
            // Given
            String codigo = "COD123";
            String nome = "Peca Teste";
            BigDecimal precoVenda = new BigDecimal("100.00");

            // When
            Peca peca = Peca.builder()
                    .codigo(codigo)
                    .nome(nome)
                    .precoVenda(precoVenda)
                    .build();

            // Then
            assertThat(peca).isNotNull();
            assertThat(peca.getId()).isNull(); // ID é gerado pela persistência
            assertThat(peca.getCodigo()).isEqualTo(codigo);
            assertThat(peca.getNome()).isEqualTo(nome);
            assertThat(peca.getDescricao()).isNull();
            assertThat(peca.getMarca()).isNull();
            assertThat(peca.getPrecoVenda()).isEqualByComparingTo(precoVenda);
            assertThat(peca.getQuantidadeEstoque()).isEqualTo(0);
            assertThat(peca.getQuantidadeMinima()).isEqualTo(1);
            assertThat(peca.getUnidade()).isEqualTo("un");
            assertThat(peca.getAtivo()).isTrue();
            assertThat(peca.getCreatedAt()).isNull(); // Gerado pela persistência
            assertThat(peca.getUpdatedAt()).isNull(); // Gerado pela persistência
            assertThat(peca.getMovimentacoes()).isEmpty();
        }

        @Test
        @DisplayName("deve criar Peca com builder e todos os valores fornecidos")
        void deveCriarPecaComBuilderEValoresFornecidos() {
            // Given
            UUID id = UUID.randomUUID();
            String codigo = "COD456";
            String nome = "Peca Completa";
            String descricao = "Descricao da peca completa";
            String marca = "Marca X";
            BigDecimal precoVenda = new BigDecimal("250.50");
            Integer quantidadeEstoque = 10;
            Integer quantidadeMinima = 5;
            String unidade = "kg";
            Boolean ativo = false;

            // When
            Peca peca = Peca.builder()
                    .id(id)
                    .codigo(codigo)
                    .nome(nome)
                    .descricao(descricao)
                    .marca(marca)
                    .precoVenda(precoVenda)
                    .quantidadeEstoque(quantidadeEstoque)
                    .quantidadeMinima(quantidadeMinima)
                    .unidade(unidade)
                    .ativo(ativo)
                    .build();

            // Then
            assertThat(peca).isNotNull();
            assertThat(peca.getId()).isEqualTo(id);
            assertThat(peca.getCodigo()).isEqualTo(codigo);
            assertThat(peca.getNome()).isEqualTo(nome);
            assertThat(peca.getDescricao()).isEqualTo(descricao);
            assertThat(peca.getMarca()).isEqualTo(marca);
            assertThat(peca.getPrecoVenda()).isEqualByComparingTo(precoVenda);
            assertThat(peca.getQuantidadeEstoque()).isEqualTo(quantidadeEstoque);
            assertThat(peca.getQuantidadeMinima()).isEqualTo(quantidadeMinima);
            assertThat(peca.getUnidade()).isEqualTo(unidade);
            assertThat(peca.getAtivo()).isFalse();
        }
    }

    @Nested
    @DisplayName("Testes de Equals e HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("deve retornar true para objetos iguais")
        void deveRetornarTrueParaObjetosIguais() {
            // Given
            UUID id = UUID.randomUUID();
            Peca peca1 = Peca.builder().id(id).codigo("A").nome("Peca A").precoVenda(BigDecimal.ONE).build();
            Peca peca2 = Peca.builder().id(id).codigo("B").nome("Peca B").precoVenda(BigDecimal.TEN).build(); // Outros atributos não importam para equals/hashCode se baseados apenas no ID

            // Then
            assertThat(peca1).isEqualTo(peca2);
            assertThat(peca1.hashCode()).isEqualTo(peca2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para objetos diferentes")
        void deveRetornarFalseParaObjetosDiferentes() {
            // Given
            Peca peca1 = Peca.builder().id(UUID.randomUUID()).codigo("A").nome("Peca A").precoVenda(BigDecimal.ONE).build();
            Peca peca2 = Peca.builder().id(UUID.randomUUID()).codigo("A").nome("Peca A").precoVenda(BigDecimal.ONE).build();

            // Then
            assertThat(peca1).isNotEqualTo(peca2);
            assertThat(peca1.hashCode()).isNotEqualTo(peca2.hashCode());
        }

        @Test
        @DisplayName("deve retornar false para objeto nulo")
        void deveRetornarFalseParaObjetoNulo() {
            // Given
            Peca peca1 = Peca.builder().id(UUID.randomUUID()).codigo("A").nome("Peca A").precoVenda(BigDecimal.ONE).build();

            // Then
            assertThat(peca1).isNotEqualTo(null);
        }

        @Test
        @DisplayName("deve retornar false para classes diferentes")
        void deveRetornarFalseParaClassesDiferentes() {
            // Given
            Peca peca1 = Peca.builder().id(UUID.randomUUID()).codigo("A").nome("Peca A").precoVenda(BigDecimal.ONE).build();
            Object obj = new Object();

            // Then
            assertThat(peca1).isNotEqualTo(obj);
        }
    }

    @Nested
    @DisplayName("Testes de Estoque")
    class EstoqueTests {

        private Peca peca;

        // Helper para criar uma peça com estoque inicial
        private Peca criarPecaComEstoque(int estoqueInicial, int quantidadeMinima) {
            return Peca.builder()
                    .codigo("P001")
                    .nome("Peca Teste")
                    .precoVenda(new BigDecimal("50.00"))
                    .quantidadeEstoque(estoqueInicial)
                    .quantidadeMinima(quantidadeMinima)
                    .build();
        }

        @Test
        @DisplayName("temEstoqueSuficiente deve retornar true quando estoque é suficiente")
        void temEstoqueSuficienteDeveRetornarTrueQuandoEstoqueSuficiente() {
            // Given
            peca = criarPecaComEstoque(10, 1);

            // When
            boolean resultado = peca.temEstoqueSuficiente(5);

            // Then
            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("temEstoqueSuficiente deve retornar true quando estoque é exatamente igual ao solicitado")
        void temEstoqueSuficienteDeveRetornarTrueQuandoEstoqueExatamenteIgual() {
            // Given
            peca = criarPecaComEstoque(10, 1);

            // When
            boolean resultado = peca.temEstoqueSuficiente(10);

            // Then
            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("temEstoqueSuficiente deve retornar false quando estoque é insuficiente")
        void temEstoqueSuficienteDeveRetornarFalseQuandoEstoqueInsuficiente() {
            // Given
            peca = criarPecaComEstoque(10, 1);

            // When
            boolean resultado = peca.temEstoqueSuficiente(15);

            // Then
            assertThat(resultado).isFalse();
        }

        @Test
        @DisplayName("decrementarEstoque deve reduzir o estoque com sucesso")
        void decrementarEstoqueDeveReduzirEstoqueComSucesso() {
            // Given
            peca = criarPecaComEstoque(10, 1);
            int quantidadeParaDecrementar = 3;
            int estoqueEsperado = 7;

            // When
            peca.decrementarEstoque(quantidadeParaDecrementar);

            // Then
            assertThat(peca.getQuantidadeEstoque()).isEqualTo(estoqueEsperado);
        }

        @Test
        @DisplayName("decrementarEstoque deve lançar IllegalStateException quando estoque insuficiente")
        void decrementarEstoqueDeveLancarExcecaoQuandoEstoqueInsuficiente() {
            // Given
            peca = criarPecaComEstoque(5, 1);
            int quantidadeParaDecrementar = 10;

            // When / Then
            assertThatThrownBy(() -> peca.decrementarEstoque(quantidadeParaDecrementar))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Estoque insuficiente para a peça 'Peca Teste'. Disponível: 5, Solicitado: 10");
            assertThat(peca.getQuantidadeEstoque()).isEqualTo(5); // Estoque não deve ser alterado
        }

        @Test
        @DisplayName("incrementarEstoque deve aumentar o estoque com sucesso")
        void incrementarEstoqueDeveAumentarEstoqueComSucesso() {
            // Given
            peca = criarPecaComEstoque(5, 1);
            int quantidadeParaIncrementar = 7;
            int estoqueEsperado = 12;

            // When
            peca.incrementarEstoque(quantidadeParaIncrementar);

            // Then
            assertThat(peca.getQuantidadeEstoque()).isEqualTo(estoqueEsperado);
        }

        @Test
        @DisplayName("estoqueBaixo deve retornar true quando estoque é menor que o mínimo")
        void estoqueBaixoDeveRetornarTrueQuandoEstoqueMenorQueMinimo() {
            // Given
            peca = criarPecaComEstoque(5, 10);

            // When
            boolean resultado = peca.estoqueBaixo();

            // Then
            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("estoqueBaixo deve retornar true quando estoque é igual ao mínimo")
        void estoqueBaixoDeveRetornarTrueQuandoEstoqueIgualAoMinimo() {
            // Given
            peca = criarPecaComEstoque(10, 10);

            // When
            boolean resultado = peca.estoqueBaixo();

            // Then
            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("estoqueBaixo deve retornar false quando estoque é maior que o mínimo")
        void estoqueBaixoDeveRetornarFalseQuandoEstoqueMaiorQueMinimo() {
            // Given
            peca = criarPecaComEstoque(15, 10);

            // When
            boolean resultado = peca.estoqueBaixo();

            // Then
            assertThat(resultado).isFalse();
        }
    }
}
