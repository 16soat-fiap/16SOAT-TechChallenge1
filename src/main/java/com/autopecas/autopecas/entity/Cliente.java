package com.autopecas.autopecas.entity;
import com.autopecas.autopecas.enums.TipoCliente;
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
        name = "clientes",
        uniqueConstraints = @UniqueConstraint(name = "uc_cliente_cpf_cnpj", columnNames = "cpf_cnpj")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"veiculos", "ordensServico"})
@EqualsAndHashCode(of = "id")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "cpf_cnpj", nullable = false)
    private String cpfCnpj;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cliente", nullable = false)
    private TipoCliente tipoCliente;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "email")
    private String email;

    @Column(name = "telefone")
    private String telefone;

    @Column(name = "ativo")
    @Builder.Default
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = false)
    private LocalDateTime updatedAt;



    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Veiculo> veiculos = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<OrdemServico> ordensServico = new ArrayList<>();

}
