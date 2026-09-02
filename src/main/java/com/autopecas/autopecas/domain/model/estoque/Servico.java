package com.autopecas.autopecas.domain.model.estoque;

import com.autopecas.autopecas.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Serviço do catálogo da oficina (troca de óleo, alinhamento, etc.).
 *
 * <p>O precoBase é o valor cobrado por padrão e vira snapshot nos itens de orçamento e de OS.
 * Serviços desativados não entram em novas OS, mas continuam vinculados às OS históricas.
 */
public final class Servico {

    private final UUID id;
    private String nome;
    private String descricao;
    private BigDecimal precoBase;
    private Integer tempoEstimadoMinutos;
    private boolean ativo;

    private Servico(UUID id, String nome, String descricao, BigDecimal precoBase,
                    Integer tempoEstimadoMinutos, boolean ativo) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessException("Nome do serviço é obrigatório");
        }
        if (precoBase == null) {
            throw new BusinessException("Preço base do serviço é obrigatório");
        }
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.precoBase = precoBase;
        this.tempoEstimadoMinutos = tempoEstimadoMinutos;
        this.ativo = ativo;
    }

    /** Novo serviço, ainda sem id. */
    public static Servico criar(String nome, String descricao, BigDecimal precoBase,
                                Integer tempoEstimadoMinutos) {
        return new Servico(null, nome, descricao, precoBase, tempoEstimadoMinutos, true);
    }

    /** Re-hidratação a partir da persistência — uso exclusivo dos mappers de adapter. */
    public static Servico reconstituir(UUID id, String nome, String descricao, BigDecimal precoBase,
                                       Integer tempoEstimadoMinutos, boolean ativo) {
        return new Servico(id, nome, descricao, precoBase, tempoEstimadoMinutos, ativo);
    }

    /**
     * Atualiza o serviço. Nome e preço base são sempre aplicados (são obrigatórios);
     * descrição e tempo estimado só quando informados.
     */
    public void atualizarDados(String nome, String descricao, BigDecimal precoBase,
                               Integer tempoEstimadoMinutos) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessException("Nome do serviço é obrigatório");
        }
        if (precoBase == null) {
            throw new BusinessException("Preço base do serviço é obrigatório");
        }
        this.nome = nome;
        this.precoBase = precoBase;
        if (descricao != null) {
            this.descricao = descricao;
        }
        if (tempoEstimadoMinutos != null) {
            this.tempoEstimadoMinutos = tempoEstimadoMinutos;
        }
    }

    public void desativar() {
        this.ativo = false;
    }

    public boolean isNovo() {
        return id == null;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPrecoBase() {
        return precoBase;
    }

    public Integer getTempoEstimadoMinutos() {
        return tempoEstimadoMinutos;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
