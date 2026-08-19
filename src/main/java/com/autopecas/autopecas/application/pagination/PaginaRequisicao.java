package com.autopecas.autopecas.application.pagination;

import java.util.List;

/**
 * Parâmetros de paginação que atravessam a fronteira da aplicação.
 *
 * <p>Existe para que Pageable do Spring Data não apareça nas ports. A tradução de e para os
 * tipos do Spring acontece nos adapters: o adapter web converte o Pageable recebido em
 * PaginaRequisicao, e o adapter de persistência a converte em PageRequest para que o
 * LIMIT/OFFSET continue sendo feito pelo banco.
 *
 * @param pagina      índice da página, começando em zero
 * @param tamanho     quantidade de itens por página
 * @param ordenacoes  critérios de ordenação, na ordem de precedência
 */
public record PaginaRequisicao(int pagina, int tamanho, List<Ordenacao> ordenacoes) {

    public PaginaRequisicao {
        if (pagina < 0) {
            throw new IllegalArgumentException("Página não pode ser negativa");
        }
        if (tamanho <= 0) {
            throw new IllegalArgumentException("Tamanho da página deve ser positivo");
        }
        ordenacoes = ordenacoes == null ? List.of() : List.copyOf(ordenacoes);
    }

    public static PaginaRequisicao de(int pagina, int tamanho) {
        return new PaginaRequisicao(pagina, tamanho, List.of());
    }

    /** Critério único de ordenação: campo do modelo e sentido. */
    public record Ordenacao(String campo, boolean ascendente) {

        public Ordenacao {
            if (campo == null || campo.isBlank()) {
                throw new IllegalArgumentException("Campo de ordenação é obrigatório");
            }
        }

        public static Ordenacao ascendente(String campo) {
            return new Ordenacao(campo, true);
        }

        public static Ordenacao descendente(String campo) {
            return new Ordenacao(campo, false);
        }
    }
}
