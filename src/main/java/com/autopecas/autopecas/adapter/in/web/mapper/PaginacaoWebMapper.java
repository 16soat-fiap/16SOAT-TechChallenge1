package com.autopecas.autopecas.adapter.in.web.mapper;

import com.autopecas.autopecas.application.pagination.Pagina;
import com.autopecas.autopecas.application.pagination.PaginaRequisicao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Fronteira de paginação entre o Spring Data e a aplicação.
 *
 * <p>Na entrada converte o Pageable em PaginaRequisicao; na saída remonta um PageImpl a partir
 * da Pagina, de modo que o envelope JSON de paginação da API continue idêntico ao anterior,
 * inclusive com o parâmetro sort.
 */
@Component
public class PaginacaoWebMapper {

    public PaginaRequisicao paraRequisicao(Pageable pageable) {
        List<PaginaRequisicao.Ordenacao> ordenacoes = pageable.getSort().stream()
                .map(ordem -> new PaginaRequisicao.Ordenacao(ordem.getProperty(), ordem.isAscending()))
                .toList();
        return new PaginaRequisicao(pageable.getPageNumber(), pageable.getPageSize(), ordenacoes);
    }

    /** Remonta o Page do Spring, convertendo cada item da página para o DTO de resposta. */
    public <T, R> Page<R> paraPage(Pagina<T> pagina, Pageable pageable, Function<T, R> conversor) {
        List<R> conteudo = pagina.conteudo().stream().map(conversor).toList();
        return new PageImpl<>(conteudo, pageable, pagina.totalElementos());
    }

    /** Pageable equivalente à requisição, útil quando o controller não recebeu um. */
    public Pageable paraPageable(PaginaRequisicao requisicao) {
        return PageRequest.of(requisicao.pagina(), requisicao.tamanho());
    }
}
