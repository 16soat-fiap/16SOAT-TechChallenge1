package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.CategoriaPeca;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Peça
 *
 * Estoque:
 *   - O saldo é mantido em cache nesta entidade para
 *     performance, mas a fonte de verdade é a tabela movimentacoes_estoque.
 *   - Toda alteração de saldo DEVE passar por uma MovimentacaoEstoque
 */
@Entity
@Table(
        name = "pecas",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_peca_codigo",       columnNames = "codigo"),
                @UniqueConstraint(name = "uk_peca_codigo_barras", columnNames = "codigo_barras")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"movimentacoes"})
@EqualsAndHashCode(of = "id")
public class Peca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "codigo", nullable = false)
    private String codigo;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    /** Marca/fabricante da peça */
    @Column(name = "marca")
    private String marca;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    private CategoriaPeca categoria;

    @Column(name = "preco_custo", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoCusto;

    @Column(name = "preco_venda", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoVenda;

    /** Saldo. tabela movimentacoes_estoque. */
    @Column(name = "quantidade_estoque", nullable = false)
    @Builder.Default
    private Integer quantidadeEstoque = 0;

    @Column(name = "quantidade_minima", nullable = false)
    @Builder.Default
    private Integer quantidadeMinima = 1;

    /** Unidade de medida (un, L, kg, m...). */
    @Column(name = "unidade", nullable = false, length = 10)
    @Builder.Default
    private String unidade = "un";

    /** OPCIONAL - PODEMOS EXCLUIR ISSO */
    @Column(name = "garantia_dias", nullable = false)
    @Builder.Default
    private Integer garantiaDias = 90;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // RELACIONAMENTOS

    @OneToMany(mappedBy = "peca", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MovimentacaoEstoque> movimentacoes = new ArrayList<>();

    // METODOS ---------------------------------------

    public boolean temEstoqueSuficiente(int quantidade) {
        return this.quantidadeEstoque >= quantidade;
    }

    /**
     * Decrementa o saldo. NÃO cria a MovimentacaoEstoque — quem chama
     * deve criar o registro de movimentação na mesma transação.
     */
    public void decrementarEstoque(int quantidade) {
        if (!temEstoqueSuficiente(quantidade)) {
            throw new IllegalStateException(
                    String.format("Estoque insuficiente para a peça '%s'. Disponível: %d, Solicitado: %d",
                            nome, quantidadeEstoque, quantidade)
            );
        }
        this.quantidadeEstoque -= quantidade;
    }

    public void incrementarEstoque(int quantidade) {
        this.quantidadeEstoque += quantidade;
    }

    public boolean estoqueBaixo() {
        return this.quantidadeEstoque <= this.quantidadeMinima;
    }
}

