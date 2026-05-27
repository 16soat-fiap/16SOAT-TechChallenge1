package com.autopecas.autopecas.entity;

import com.autopecas.autopecas.enums.StatusOS;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cliente", "veiculo", "itensServico", "itensPeca", "historico"})
@EqualsAndHashCode(of = "id")
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "numero_os", nullable = false)
    private String numeroOs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private StatusOS statusOS = StatusOS.RECEBIDA;

    @Column(name = "observacoes_entrada", length = 1000)
    private String observacoesEntrada;

    @Column(name = "diagnostico", length = 2000)
    private String diagnostico;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "data_envio_orcamento")
    private LocalDateTime dataEnvioOrcamento;

    @Column(name = "data_aprovacao_orcamento")
    private LocalDateTime dataAprovacaoOrcamento;

    @Column(name = "orcamento_aprovado")
    private Boolean orcamentoAprovado;

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



    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_os_cliente"))
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_os_veiculo"))
    private Veiculo veiculo;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemServicoOS> itensServico = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemPecaOS> itensPeca = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    @Builder.Default
    private List<HistoricoStatusOS> historico = new ArrayList<>();





}
