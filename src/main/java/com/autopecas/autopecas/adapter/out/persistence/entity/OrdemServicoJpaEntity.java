package com.autopecas.autopecas.adapter.out.persistence.entity;

import com.autopecas.autopecas.domain.enums.StatusOS;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade JPA da Ordem de Serviço.
 *
 * <p>Só as coleções que pertencem ao agregado (itens de serviço e de peça) permanecem
 * associadas, com cascade e orphanRemoval. As coleções de orçamentos e de histórico foram
 * removidas de propósito: são agregados próprios com FK obrigatória, e mantê-las com cascade
 * faria o merge de uma OS tentar anular essas FKs.
 *
 * <p>Cliente, veículo, atendentes e mecânico são referenciados apenas pela coluna de FK.
 */
@Entity
@Table(
        name = "ordens_servico",
        uniqueConstraints = @UniqueConstraint(name = "uk_os_numero", columnNames = "numero"),
        indexes = {
                @Index(name = "idx_os_cliente", columnList = "cliente_id"),
                @Index(name = "idx_os_veiculo", columnList = "veiculo_id"),
                @Index(name = "idx_os_status", columnList = "status"),
                @Index(name = "idx_os_mecanico", columnList = "mecanico_responsavel_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class OrdemServicoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "numero", nullable = false, updatable = false)
    private String numero;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusOS status;

    @Column(name = "quilometragem_entrada")
    private Integer quilometragemEntrada;

    @Column(name = "observacoes_entrada", length = 1000)
    private String observacoesEntrada;

    @Column(name = "diagnostico", length = 2000)
    private String diagnostico;

    @Column(name = "queixa_cliente", length = 1000)
    private String queixaCliente;

    @Column(name = "valor_total_aprovado", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotalAprovado;

    @Column(name = "data_inicio_execucao")
    private LocalDateTime dataInicioExecucao;

    @Column(name = "data_finalizacao")
    private LocalDateTime dataFinalizacao;

    @Column(name = "data_entrega")
    private LocalDateTime dataEntrega;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "veiculo_id", nullable = false)
    private UUID veiculoId;

    @Column(name = "atendente_recepcao_id")
    private UUID atendenteRecepcaoId;

    @Column(name = "atendente_entrega_id")
    private UUID atendenteEntregaId;

    @Column(name = "mecanico_responsavel_id")
    private UUID mecanicoResponsavelId;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ItemServicoOSJpaEntity> itensServico = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ItemPecaOSJpaEntity> itensPeca = new ArrayList<>();

    /** Substitui os itens de serviço preservando a instância da coleção (exigência do Hibernate). */
    public void substituirItensServico(List<ItemServicoOSJpaEntity> novos) {
        this.itensServico.clear();
        novos.forEach(item -> {
            item.setOrdemServico(this);
            this.itensServico.add(item);
        });
    }

    /** Substitui os itens de peça preservando a instância da coleção. */
    public void substituirItensPeca(List<ItemPecaOSJpaEntity> novos) {
        this.itensPeca.clear();
        novos.forEach(item -> {
            item.setOrdemServico(this);
            this.itensPeca.add(item);
        });
    }
}
