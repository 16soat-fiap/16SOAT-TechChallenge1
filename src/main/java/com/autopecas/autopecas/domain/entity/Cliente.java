package com.autopecas.autopecas.domain.entity;
import com.autopecas.autopecas.domain.enums.TipoCliente;
import com.autopecas.autopecas.domain.valueobject.Endereco;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade abstrata base para clientes
 *   - Tabela "clientes" guarda campos comuns.
 *   - Tabelas "clientes_pf" e "clientes_pj" guardam campos específicos.
 *
 * Subclasses:
 *   - ClientePF cpf, dataNascimento, rg
 *   - ClientePJ  cnpj, razaoSocial
 *
 * Regras comuns:
 *   - email é único
 *   - Cliente nunca é deletado; usar ativo = false
 *   - Documento (cpf/cnpj) é validado na camada de aplicação.
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
@AllArgsConstructor
@ToString(exclude = {"veiculos", "ordensServico"})
@EqualsAndHashCode(of = "id")
@SuperBuilder
public abstract class Cliente  {

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
    @Builder.Default
    private Boolean aceitaNotificacoes = true;

    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

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
    private Endereco endereco;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Veiculo> veiculos = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrdemServico> ordensServico = new ArrayList<>();

    //metodos
    /* dessa forma podemos separar os atributos, cpf e cnpj em coisas distintas, caso seja pf pega o CPF
    caso seja pj pega o CNPJ.
     */
    public abstract String getDocumento();

    //retorna PF ou PJ a depender do tipo do cliente
    public abstract TipoCliente getTipo();

}
