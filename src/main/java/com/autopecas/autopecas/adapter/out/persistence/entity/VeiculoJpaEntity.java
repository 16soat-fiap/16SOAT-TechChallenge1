package com.autopecas.autopecas.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA do veículo.
 *
 * <p>A placa é coluna VARCHAR simples (o Value Object Placa fica no domínio) e o cliente é
 * referenciado pela coluna de FK, sem associação ManyToOne — Veiculo e Cliente são agregados
 * distintos.
 */
@Entity
@Table(
        name = "veiculos",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_veiculo_placa", columnNames = "placa"),
                @UniqueConstraint(name = "uk_veiculo_chassi", columnNames = "chassi"),
                @UniqueConstraint(name = "uk_veiculo_renavam", columnNames = "renavam")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class VeiculoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "placa", nullable = false, length = 7)
    private String placa;

    @Column(name = "chassi", length = 17)
    private String chassi;

    @Column(name = "renavam")
    private String renavam;

    @Column(name = "marca", nullable = false, length = 60)
    private String marca;

    @Column(name = "modelo", nullable = false, length = 100)
    private String modelo;

    @Column(name = "ano_modelo")
    private Integer anoModelo;

    @Column(name = "cor")
    private String cor;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
