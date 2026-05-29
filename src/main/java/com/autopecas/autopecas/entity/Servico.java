package com.autopecas.autopecas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Catálogo de serviços executados pela oficina (ex.: troca de óleo, alinhamento).
 *
 * Regras de negócio:
 * - precoBase é o valor cobrado por padrão; pode ser sobrescrito no ItemServicoOS.
 * - tempoEstimadoMinutos é usado para calcular o tempo médio de execução.
 * - Serviços desativados (ativo = false) não podem ser adicionados a novas OS,
 *   mas permanecem vinculados a OS históricas.
 */
@Entity
@Table(name = "servicos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "preco_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoBase;

    /** Duração estimada em minutos para fins de monitoramento de tempo médio. */
    @Column(name = "tempo_estimado_minutos")
    private Integer tempoEstimadoMinutos;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
