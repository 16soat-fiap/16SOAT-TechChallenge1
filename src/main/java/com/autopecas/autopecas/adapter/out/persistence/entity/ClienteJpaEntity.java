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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA do cliente — detalhe de persistência, sem regra de negócio.
 *
 * <p>As coleções inversas (veiculos, ordensServico) do modelo anterior foram removidas: são
 * agregados próprios, referenciados por id. Mantê-las com cascade faria o merge de um cliente
 * tentar desassociar veículos existentes.
 *
 */
@Entity
@Table(
        name = "clientes",
        uniqueConstraints = @UniqueConstraint(name = "uk_cliente_email", columnNames = "email")
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo_cliente", discriminatorType = DiscriminatorType.STRING, length = 2)
@Getter
@Setter
@NoArgsConstructor
public abstract class ClienteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "email")
    private String email;

    @Column(name = "telefone")
    private String telefone;

    @Column(name = "aceita_notificacoes", nullable = false)
    private Boolean aceitaNotificacoes;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "cep", column = @Column(name = "endereco_cep")),
            @AttributeOverride(name = "logradouro", column = @Column(name = "endereco_logradouro")),
            @AttributeOverride(name = "numero", column = @Column(name = "endereco_numero")),
            @AttributeOverride(name = "complemento", column = @Column(name = "endereco_complemento")),
            @AttributeOverride(name = "bairro", column = @Column(name = "endereco_bairro")),
            @AttributeOverride(name = "cidade", column = @Column(name = "endereco_cidade")),
            @AttributeOverride(name = "uf", column = @Column(name = "endereco_uf"))
    })
    private EnderecoEmbeddable endereco;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = false)
    private LocalDateTime updatedAt;
}
