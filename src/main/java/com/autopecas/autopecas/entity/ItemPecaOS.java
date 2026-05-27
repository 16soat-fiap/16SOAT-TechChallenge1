package com.autopecas.autopecas.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "itens_peca_os")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ordemServico")
@EqualsAndHashCode(of = "id")
public class ItemPecaOS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false, foreignKey = @ForeignKey(name = "fk_item_peca_os"))
    private OrdemServico ordemServico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "peca_id", nullable = false, foreignKey = @ForeignKey(name = "fk_item_peca_catalogo"))
    private Peca peca;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

}
