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

    /**
     * Lista as OS conforme o filtro.
     *
     * <p>Quando {@link Filtro#status()} é nulo, o resultado é a <b>fila de trabalho</b>: exclui
     * logicamente as OS encerradas ({@link StatusOS#encerrouAtendimento()}) e ordena por
     * {@link StatusOS#prioridadeNaFila()}, com as mais antigas primeiro dentro de cada faixa.
     * Nesse modo a ordenação é fixa e ignora o sort da requisição — a prioridade da oficina não
     * é preferência de quem chama.
     */
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

        /** Sem status explícito, a listagem é a fila: encerradas fora, prioridade aplicada. */
        public boolean ehFilaDeTrabalho() {
            return status == null;
        }
    }
}
