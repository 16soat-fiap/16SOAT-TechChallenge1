package com.autopecas.autopecas.adapter.out.persistence.adapter;

import com.autopecas.autopecas.adapter.out.persistence.projection.OrdemServicoResumo;
import com.autopecas.autopecas.adapter.out.persistence.repository.OrdemServicoJpaRepository;
import com.autopecas.autopecas.application.pagination.Pagina;
import com.autopecas.autopecas.application.pagination.PaginaRequisicao;
import com.autopecas.autopecas.application.port.in.view.OrdemServicoView;
import com.autopecas.autopecas.application.port.out.ConsultaOrdemServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter da query port de Ordens de Serviço.
 *
 * <p>Converte a PaginaRequisicao da aplicação em Pageable do Spring Data — é aqui, e só aqui,
 * que o LIMIT/OFFSET é decidido. Os filtros seguem a mesma precedência da versão anterior:
 * status, depois cliente, depois mecânico, senão todas.
 */
@Component
public class ConsultaOrdemServicoAdapter implements ConsultaOrdemServico {

    private final OrdemServicoJpaRepository repository;

    public ConsultaOrdemServicoAdapter(OrdemServicoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Pagina<OrdemServicoView> listar(Filtro filtro, PaginaRequisicao paginacao) {
        Pageable pageable = paraPageable(paginacao);

        Page<OrdemServicoResumo> pagina;
        if (filtro.status() != null) {
            pagina = repository.buscarResumosPorStatus(filtro.status(), pageable);
        } else if (filtro.clienteId() != null) {
            pagina = repository.buscarResumosPorCliente(filtro.clienteId(), pageable);
        } else if (filtro.mecanicoId() != null) {
            pagina = repository.buscarResumosPorMecanico(filtro.mecanicoId(), pageable);
        } else {
            pagina = repository.buscarResumos(pageable);
        }

        return new Pagina<>(pagina.getContent().stream().map(this::paraView).toList(),
                paginacao.pagina(), paginacao.tamanho(), pagina.getTotalElements());
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
