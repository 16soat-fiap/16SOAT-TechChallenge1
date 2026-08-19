package com.autopecas.autopecas.adapter.out.persistence.entity;

import com.autopecas.autopecas.domain.enums.StatusItemOS;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Entidade JPA do item de peça da OS — filho do agregado OrdemServico. */
@Entity
@Table(name = "itens_peca_os")
@Getter
@Setter
@NoArgsConstructor
public class ItemPecaOSJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_peca_os"))
    private OrdemServicoJpaEntity ordemServico;

    @Column(name = "peca_id", nullable = false)
    private UUID pecaId;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusItemOS status;

    @Column(name = "instalado_por_id")
    private UUID instaladoPorId;

    @Column(name = "data_instalacao")
    private LocalDateTime dataInstalacao;
}
