package com.autopecas.autopecas.adapter.out.persistence.adapter;

import com.autopecas.autopecas.adapter.out.persistence.projection.OrdemServicoResumo;
import com.autopecas.autopecas.adapter.out.persistence.repository.OrdemServicoJpaRepository;
import com.autopecas.autopecas.application.pagination.Pagina;
import com.autopecas.autopecas.application.pagination.PaginaRequisicao;
import com.autopecas.autopecas.application.port.in.view.OrdemServicoView;
import com.autopecas.autopecas.application.port.out.ConsultaOrdemServico;
import com.autopecas.autopecas.domain.enums.StatusOS;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter da query port de Ordens de Serviço.
 *
 * <p>Converte a PaginaRequisicao da aplicação em Pageable do Spring Data — é aqui, e só aqui,
 * que o LIMIT/OFFSET é decidido.
 *
 * <p>Há dois modos de listagem. Com status explícito, a consulta devolve exatamente aquele
 * status, encerrado ou não, respeitando o sort da requisição. Sem status, o resultado é a fila
 * de trabalho: encerradas fora e ordem fixa por prioridade. Cliente e mecânico apenas estreitam
 * o recorte, nessa ordem de precedência.
 */
@Component
public class ConsultaOrdemServicoAdapter implements ConsultaOrdemServico {

    private final OrdemServicoJpaRepository repository;

    public ConsultaOrdemServicoAdapter(OrdemServicoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Pagina<OrdemServicoView> listar(Filtro filtro, PaginaRequisicao paginacao) {
        Page<OrdemServicoResumo> pagina = filtro.ehFilaDeTrabalho()
                ? buscarFila(filtro, paginacao)
                : repository.buscarResumosPorStatus(filtro.status(), paraPageable(paginacao));

        return new Pagina<>(pagina.getContent().stream().map(this::paraView).toList(),
                paginacao.pagina(), paginacao.tamanho(), pagina.getTotalElements());
    }

    /**
     * Fila de trabalho: as OS encerradas ficam de fora e a ordem é a prioridade declarada em
     * StatusOS, com as mais antigas primeiro dentro de cada faixa.
     *
     * <p>O Pageable vai deliberadamente sem sort. O Spring Data anexa o sort da requisição
     * <i>depois</i> do ORDER BY da query, o que rebaixaria a prioridade da oficina a mero
     * critério de desempate — e a ordem da fila não é preferência de quem chama.
     */
    private Page<OrdemServicoResumo> buscarFila(Filtro filtro, PaginaRequisicao paginacao) {
        Pageable pageable = PageRequest.of(paginacao.pagina(), paginacao.tamanho());
        List<StatusOS> emAndamento = StatusOS.emAndamento();

        if (filtro.clienteId() != null) {
            return repository.buscarFilaPorCliente(emAndamento, filtro.clienteId(), pageable);
        }
        if (filtro.mecanicoId() != null) {
            return repository.buscarFilaPorMecanico(emAndamento, filtro.mecanicoId(), pageable);
        }
        return repository.buscarFila(emAndamento, pageable);
    }

    @Override
    public Optional<OrdemServicoView> porNumero(String numero) {
        return repository.buscarResumoPorNumero(numero).map(this::paraView);
    }

    @Override
    public Optional<OrdemServicoView> porId(UUID id) {
        return repository.buscarResumoPorId(id).map(this::paraView);
    }

    private Pageable paraPageable(PaginaRequisicao paginacao) {
        if (paginacao.ordenacoes().isEmpty()) {
            return PageRequest.of(paginacao.pagina(), paginacao.tamanho());
        }
        Sort sort = Sort.by(paginacao.ordenacoes().stream()
                .map(ordenacao -> ordenacao.ascendente()
                        ? Sort.Order.asc(ordenacao.campo())
                        : Sort.Order.desc(ordenacao.campo()))
                .toList());
        return PageRequest.of(paginacao.pagina(), paginacao.tamanho(), sort);
    }

    private OrdemServicoView paraView(OrdemServicoResumo resumo) {
        return new OrdemServicoView(resumo.id(), resumo.numero(),
                resumo.status() != null ? resumo.status().name() : null, resumo.queixaCliente(),
                resumo.diagnostico(), resumo.valorTotalAprovado(), resumo.criadoEm(),
                resumo.clienteId(), resumo.clienteNome(), resumo.veiculoId(), resumo.veiculoPlaca());
    }
}
