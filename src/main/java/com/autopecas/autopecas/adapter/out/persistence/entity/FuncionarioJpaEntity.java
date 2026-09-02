package com.autopecas.autopecas.adapter.out.persistence.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Entidade JPA do funcionário. As coleções inversas de OS foram removidas. */
@Entity
@Table(
        name = "funcionarios",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_funcionario_matricula", columnNames = "matricula"),
                @UniqueConstraint(name = "uk_funcionario_cpf", columnNames = "cpf")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo_funcionario", discriminatorType = DiscriminatorType.STRING, length = 20)
@Getter
@Setter
@NoArgsConstructor
public abstract class FuncionarioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "matricula", nullable = false, updatable = false)
    private String matricula;

    @Column(name = "cpf", nullable = false, updatable = false)
    private String cpf;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "email")
    private String email;

    @Column(name = "telefone")
    private String telefone;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "cep", column = @Column(name = "endereco_cep", length = 9)),
            @AttributeOverride(name = "logradouro", column = @Column(name = "endereco_logradouro", length = 200)),
            @AttributeOverride(name = "numero", column = @Column(name = "endereco_numero", length = 20)),
            @AttributeOverride(name = "complemento", column = @Column(name = "endereco_complemento", length = 100)),
            @AttributeOverride(name = "bairro", column = @Column(name = "endereco_bairro", length = 100)),
            @AttributeOverride(name = "cidade", column = @Column(name = "endereco_cidade", length = 100)),
            @AttributeOverride(name = "uf", column = @Column(name = "endereco_uf", length = 2))
    })
    private EnderecoEmbeddable endereco;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
