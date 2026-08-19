package com.autopecas.autopecas.application.pagination;

import java.util.List;
import java.util.function.Function;

/**
 * Fatia de resultado devolvida pelas ports de consulta paginada.
 *
 * <p>Carrega apenas o necessário para o adapter web remontar o envelope de paginação do
 * Spring, mantendo o JSON da API idêntico.
 *
 * @param conteudo        itens da página atual
 * @param pagina          índice da página, começando em zero
 * @param tamanho         quantidade de itens por página solicitada
 * @param totalElementos  total de itens em todas as páginas
 */
public record Pagina<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos) {

    public Pagina {
        conteudo = conteudo == null ? List.of() : List.copyOf(conteudo);
    }

    public static <T> Pagina<T> vazia(PaginaRequisicao requisicao) {
        return new Pagina<>(List.of(), requisicao.pagina(), requisicao.tamanho(), 0L);
    }

    /** Converte o conteúdo preservando os metadados da página. */
    public <R> Pagina<R> mapear(Function<T, R> conversor) {
        return new Pagina<>(conteudo.stream().map(conversor).toList(), pagina, tamanho, totalElementos);
    }
}
