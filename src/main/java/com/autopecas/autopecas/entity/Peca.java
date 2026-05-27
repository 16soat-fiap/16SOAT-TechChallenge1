package com.autopecas.autopecas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "pecas",
        uniqueConstraints = @UniqueConstraint(name = "uk_peca_codigo", columnNames = "codigo_peca")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class Peca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** A DEFINIR COM A EQUIPE COMO SERÁ CATALOGADA AS PEÇAS **/
    @Column(name = "codigo_peca", nullable = false)
    private String codigoPeca;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "preco_custo", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoCusto;

    @Column(name = "preco_venda", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoVenda;


    @Column(name = "quantidade_estoque", nullable = false)
    @Builder.Default
    private Integer quantidadeEstoque = 0;


    @Column(name = "quantidade_minima", nullable = false)
    @Builder.Default
    private Integer quantidadeMinima = 1;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
