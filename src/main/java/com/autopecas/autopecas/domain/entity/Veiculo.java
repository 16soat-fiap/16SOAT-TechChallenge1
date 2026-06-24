package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.valueobject.Placa;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Veículo de um cliente.
 *
 * Regras:
 *   - placa normalizada em maiúsculas sem hífen (Mercosul ou antigo).
 *   - chassi (VIN) tem 17 caracteres alfanuméricos — único.
 *   - renavam tem até 11 dígitos — único.
 *   - quilometragemAtual é atualizada a cada nova OS (snapshot da última entrada).
 *   - Um veículo pertence a UM cliente, mas pode ser transferido (alterar FK).
 */
@Entity
@Table(
        name = "veiculos",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_veiculo_placa",   columnNames = "placa"),
                @UniqueConstraint(name = "uk_veiculo_chassi",  columnNames = "chassi"),
                @UniqueConstraint(name = "uk_veiculo_renavam", columnNames = "renavam")
        }
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

    /** Placa normalizada (sem hífen, maiúsculas). Aceita formato antigo (ABC1234) ou Mercosul (ABC1D23). */
    @Embedded
    @Column(name = "placa", nullable = false, length = 7)
    private Placa placa;

    /** Número do chassi (VIN). 17 caracteres alfanuméricos sem I, O, Q. */
    @Column(name = "chassi", length = 17)
    private String chassi;

    /** RENAVAM — até 11 dígitos. NÃO SEI SE É NECESSARIO */
    @Column(name = "renavam")
    private String renavam;

    @Column(name = "marca", nullable = false, length = 60)
    private String marca;

    @Column(name = "modelo", nullable = false, length = 100)
    private String modelo;

    /** Ano modelo (pode diferir do ano de fabricação). */
    @Column(name = "ano_modelo")
    private Integer anoModelo;

    @Column(name = "cor")
    private String cor;

    /** Observações do veículo */
    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    // ─── Relacionamentos

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_veiculo_cliente"))
    private Cliente cliente;

    @OneToMany(mappedBy = "veiculo", fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrdemServico> ordensServico = new ArrayList<>();

    public String getPlaca() {
        return placa.getValor();
    }
}
