package com.autopecas.autopecas.domain.entity;

import com.autopecas.autopecas.domain.enums.TipoFuncionario;
import com.autopecas.autopecas.domain.valueobject.Endereco;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade abstrata base para os funcionários da oficina
 * funcionarios guarda os campos (matricula, etc...)
 * Subclasses guardam campos específicos do papel
 * Subclasses:
 * -Mecanico: executa o processo/manutenção.
 * -Atendente: inicia as OS, autorização para descontos.
 * -Gestor: área de gestão.
 *
 * matricula é única, imutável e usada como crachá.
 */

@Entity
@Table(
        name = "funcionarios",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_funcionario_matricula", columnNames = "matricula"),
                @UniqueConstraint(name = "uk_funcionario_cpf",       columnNames = "cpf"),
                @UniqueConstraint(name = "uk_funcionario_usuario",   columnNames = "usuario_id")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo_funcionario", discriminatorType = DiscriminatorType.STRING, length = 20)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "usuario")
@EqualsAndHashCode(of = "id")
public abstract class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Matricula para identificar o funcionário
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
    private Boolean ativo = true;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "cep",         column = @Column(name = "endereco_cep",         length = 9)),
            @AttributeOverride(name = "logradouro",  column = @Column(name = "endereco_logradouro",  length = 200)),
            @AttributeOverride(name = "numero",      column = @Column(name = "endereco_numero",      length = 20)),
            @AttributeOverride(name = "complemento", column = @Column(name = "endereco_complemento", length = 100)),
            @AttributeOverride(name = "bairro",      column = @Column(name = "endereco_bairro",      length = 100)),
            @AttributeOverride(name = "cidade",      column = @Column(name = "endereco_cidade",      length = 100)),
            @AttributeOverride(name = "uf",          column = @Column(name = "endereco_uf",          length = 2))
    })
    private Endereco endereco;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;



    //  Métodos───────────────────────

    public abstract TipoFuncionario getTipo();

    // Identificação para históricos: Carlos (FUN-0042)
    public String getIdentificacao() {
        return String.format("%s (%s)", nome, matricula);
    }
}


