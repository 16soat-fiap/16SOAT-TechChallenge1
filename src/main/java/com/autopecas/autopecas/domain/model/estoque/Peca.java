package com.autopecas.autopecas.domain.model.estoque;

import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.EstoqueInsuficienteException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Peça do catálogo, com o saldo de estoque em cache.
 *
 * <p>A fonte de verdade do saldo é a sequência de MovimentacaoEstoque; o campo
 * quantidadeEstoque é o acumulado mantido para consulta rápida. Toda alteração de saldo
 * deve passar pelo domain service MovimentadorDeEstoque, que garante o par
 * "mutação da peça + registro da movimentação".
 */
public final class Peca {

    private final UUID id;
    private final String codigo;
    private String nome;
    private String descricao;
    private String marca;
    private BigDecimal precoVenda;
    private int quantidadeEstoque;
    private int quantidadeMinima;
    private String unidade;
    private boolean ativo;
    private final LocalDateTime criadoEm;
    private final LocalDateTime atualizadoEm;

    private Peca(UUID id, String codigo, String nome, String descricao, String marca,
                 BigDecimal precoVenda, int quantidadeEstoque, int quantidadeMinima, String unidade,
                 boolean ativo, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("Código da peça é obrigatório");
        }
        if (nome == null || nome.isBlank()) {
            throw new BusinessException("Nome da peça é obrigatório");
        }
        if (precoVenda == null) {
            throw new BusinessException("Preço de venda da peça é obrigatório");
        }
        this.id = id;
        this.codigo = codigo;
        this.nome = nome;
        this.descricao = descricao;
        this.marca = marca;
        this.precoVenda = precoVenda;
        this.quantidadeEstoque = quantidadeEstoque;
        this.quantidadeMinima = quantidadeMinima;
        this.unidade = unidade;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    /** Nova peça, ainda sem id e com saldo zero — o estoque inicial entra por movimentação. */
    public static Peca criar(String codigo, String nome, String descricao, BigDecimal precoVenda,
                             int quantidadeMinima, String unidade) {
        return new Peca(null, codigo, nome, descricao, null, precoVenda, 0, quantidadeMinima,
                unidade, true, null, null);
    }

    /** Re-hidratação a partir da persistência — uso exclusivo dos mappers de adapter. */
    public static Peca reconstituir(UUID id, String codigo, String nome, String descricao, String marca,
                                    BigDecimal precoVenda, int quantidadeEstoque, int quantidadeMinima,
                                    String unidade, boolean ativo, LocalDateTime criadoEm,
                                    LocalDateTime atualizadoEm) {
        return new Peca(id, codigo, nome, descricao, marca, precoVenda, quantidadeEstoque,
                quantidadeMinima, unidade, ativo, criadoEm, atualizadoEm);
    }

    public boolean temEstoqueSuficiente(int quantidade) {
        return this.quantidadeEstoque >= quantidade;
    }

    /**
     * Baixa o saldo. Não cria a movimentação — quem chama deve registrá-la na mesma
     * transação (ver MovimentadorDeEstoque).
     */
    public void decrementarEstoque(int quantidade) {
        if (!temEstoqueSuficiente(quantidade)) {
            throw new EstoqueInsuficienteException(nome, quantidadeEstoque, quantidade);
        }
        this.quantidadeEstoque -= quantidade;
    }

    public void incrementarEstoque(int quantidade) {
        this.quantidadeEstoque += quantidade;
    }

    public boolean estoqueBaixo() {
        return this.quantidadeEstoque <= this.quantidadeMinima;
    }

    /** Atualização parcial: campos nulos (ou textos em branco) são ignorados. */
    public void atualizarDados(String nome, String descricao, BigDecimal precoVenda,
                               Integer quantidadeMinima, String unidade) {
        if (nome != null && !nome.isBlank()) {
            this.nome = nome;
        }
        if (descricao != null) {
            this.descricao = descricao;
        }
        if (precoVenda != null) {
            this.precoVenda = precoVenda;
        }
        if (quantidadeMinima != null) {
            this.quantidadeMinima = quantidadeMinima;
        }
        if (unidade != null && !unidade.isBlank()) {
            this.unidade = unidade;
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

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getMarca() {
        return marca;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public int getQuantidadeMinima() {
        return quantidadeMinima;
    }

    public String getUnidade() {
        return unidade;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
