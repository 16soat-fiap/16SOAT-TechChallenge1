package com.autopecas.autopecas.application.port.out;

import com.autopecas.autopecas.application.pagination.Pagina;
import com.autopecas.autopecas.application.pagination.PaginaRequisicao;
import com.autopecas.autopecas.application.port.in.view.OrdemServicoView;
import com.autopecas.autopecas.domain.enums.StatusOS;

import java.util.Optional;
import java.util.UUID;

/**
 * Query port de leitura de Ordens de Serviço.
 *
 * <p>Não passa pelo agregado: o adapter monta a OrdemServicoView direto de uma projeção SQL,
 * que já traz o nome do cliente e a placa do veículo. É o que evita o N+1 que existiria se a
 * listagem carregasse cada agregado e depois buscasse cliente e veículo um a um.
 */
public interface ConsultaOrdemServico {

    Pagina<OrdemServicoView> listar(Filtro filtro, PaginaRequisicao paginacao);

    Optional<OrdemServicoView> porNumero(String numero);

    Optional<OrdemServicoView> porId(UUID id);

    /**
     * Filtros mutuamente exclusivos, aplicados na ordem status, cliente, mecânico — mesma
     * precedência da versão anterior ao refactor.
     */
    record Filtro(StatusOS status, UUID clienteId, UUID mecanicoId) {

        public static Filtro vazio() {
            return new Filtro(null, null, null);
        }
    }
}
