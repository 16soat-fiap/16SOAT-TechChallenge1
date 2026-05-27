package com.autopecas.autopecas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "veiculos",
        uniqueConstraints = @UniqueConstraint(name = "uc_veiculo_placa", columnNames = "placa")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cliente", "ordensServico"})
@EqualsAndHashCode(of = "id")
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "placa", nullable = false)
    private String placa;

    @Column(name = "marca", nullable = false)
    private String marca;

    @Column(name = "modelo", nullable = false)
    private String modelo;

    @Column(name = "ano")
    private Integer ano;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;



    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "veiculo_cliente"))
    private Cliente cliente;

    @OneToMany(mappedBy = "veiculo", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<OrdemServico> ordensServico = new ArrayList<>();

}
