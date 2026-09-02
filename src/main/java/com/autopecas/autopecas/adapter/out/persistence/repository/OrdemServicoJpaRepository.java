package com.autopecas.autopecas.adapter.out.persistence.repository;

import com.autopecas.autopecas.adapter.out.persistence.entity.OrdemServicoJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.projection.ExecucaoConcluida;
import com.autopecas.autopecas.adapter.out.persistence.projection.OrdemServicoResumo;
import com.autopecas.autopecas.domain.enums.StatusOS;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data da Ordem de Serviço.
 *
 * <p>As consultas de leitura devolvem a projeção OrdemServicoResumo por constructor expression,
 * com join explícito em clientes e veiculos. Como as entidades não têm associações entre
 * agregados, o join é feito por ON — suportado pelo Hibernate 6 para entidades não relacionadas.
 *
 * <p>Há um método por combinação de filtro em vez de uma consulta com parâmetros opcionais, o
 * que preserva exatamente a precedência de filtros da versão anterior ao refactor.
 */
public interface OrdemServicoJpaRepository extends JpaRepository<OrdemServicoJpaEntity, UUID> {

    String SELECT_RESUMO = """
            SELECT new com.autopecas.autopecas.adapter.out.persistence.projection.OrdemServicoResumo(
                os.id, os.numero, os.status, os.queixaCliente, os.diagnostico,
                os.valorTotalAprovado, os.createdAt, os.clienteId, c.nome, os.veiculoId, v.placa)
            FROM OrdemServicoJpaEntity os
            JOIN ClienteJpaEntity c ON c.id = os.clienteId
            JOIN VeiculoJpaEntity v ON v.id = os.veiculoId
            """;

    String COUNT_RESUMO = "SELECT COUNT(os) FROM OrdemServicoJpaEntity os";

    /**
     * Ordenação da fila de trabalho, materializando StatusOS.prioridadeNaFila():
     * EM_EXECUCAO → AGUARDANDO_APROVACAO → EM_DIAGNOSTICO → RECEBIDA, e dentro de cada faixa as
     * mais antigas primeiro.
     *
     * <p>Os números repetem o que o enum declara. ArquiteturaFilaDeTrabalhoTest compara os dois
     * lados e quebra a build se divergirem — é o que evita que mexer no enum deixe a query para trás.
     */
    String ORDER_BY_FILA = """
             ORDER BY CASE os.status
                 WHEN com.autopecas.autopecas.domain.enums.StatusOS.EM_EXECUCAO THEN 1
                 WHEN com.autopecas.autopecas.domain.enums.StatusOS.AGUARDANDO_APROVACAO THEN 2
                 WHEN com.autopecas.autopecas.domain.enums.StatusOS.EM_DIAGNOSTICO THEN 3
                 WHEN com.autopecas.autopecas.domain.enums.StatusOS.RECEBIDA THEN 4
                 ELSE 5
             END ASC, os.createdAt ASC
            """;

    /** Recorte da fila: só os status que ainda demandam ação — exclusão lógica das encerradas. */
    String WHERE_EM_ANDAMENTO = " WHERE os.status IN :statusEmAndamento";

    /**
     * Carrega a OS para escrita forçando o incremento da versão no commit.
     *
     * <p>Como o agregado de domínio é destacado da entidade JPA, o adapter recarrega a linha
     * antes de gravar — e uma comparação de versões seria enganosa, porque um flush intermediário
     * na mesma transação já teria avançado a versão. FORCE_INCREMENT resolve no lugar certo: o
     * conflito é detectado pelo banco, entre transações, no commit.
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT os FROM OrdemServicoJpaEntity os WHERE os.id = :id")
    Optional<OrdemServicoJpaEntity> porIdParaAtualizacao(@Param("id") UUID id);

    Optional<OrdemServicoJpaEntity> findByNumero(String numero);

    @Query(value = "SELECT nextval('os_numero_seq')", nativeQuery = true)
    Long proximoNumero();

    /**
     * Busca por status explícito — único caminho que enxerga OS já encerradas, e o único que
     * respeita o sort informado na requisição.
     */
    @Query(value = SELECT_RESUMO + " WHERE os.status = :status",
            countQuery = COUNT_RESUMO + " WHERE os.status = :status")
    Page<OrdemServicoResumo> buscarResumosPorStatus(@Param("status") StatusOS status, Pageable pageable);

    // ─── Fila de trabalho: exclui FINALIZADA/ENTREGUE e ordena por prioridade ───────────────

    @Query(value = SELECT_RESUMO + WHERE_EM_ANDAMENTO + ORDER_BY_FILA,
            countQuery = COUNT_RESUMO + WHERE_EM_ANDAMENTO)
    Page<OrdemServicoResumo> buscarFila(@Param("statusEmAndamento") List<StatusOS> statusEmAndamento,
                                        Pageable pageable);

    @Query(value = SELECT_RESUMO + WHERE_EM_ANDAMENTO + " AND os.clienteId = :clienteId"
            + ORDER_BY_FILA,
            countQuery = COUNT_RESUMO + WHERE_EM_ANDAMENTO + " AND os.clienteId = :clienteId")
    Page<OrdemServicoResumo> buscarFilaPorCliente(
            @Param("statusEmAndamento") List<StatusOS> statusEmAndamento,
            @Param("clienteId") UUID clienteId,
            Pageable pageable);

    @Query(value = SELECT_RESUMO + WHERE_EM_ANDAMENTO + " AND os.mecanicoResponsavelId = :mecanicoId"
            + ORDER_BY_FILA,
            countQuery = COUNT_RESUMO + WHERE_EM_ANDAMENTO
                    + " AND os.mecanicoResponsavelId = :mecanicoId")
    Page<OrdemServicoResumo> buscarFilaPorMecanico(
            @Param("statusEmAndamento") List<StatusOS> statusEmAndamento,
            @Param("mecanicoId") UUID mecanicoId,
            Pageable pageable);

    @Query(SELECT_RESUMO + " WHERE os.numero = :numero")
    Optional<OrdemServicoResumo> buscarResumoPorNumero(@Param("numero") String numero);

    @Query(SELECT_RESUMO + " WHERE os.id = :id")
    Optional<OrdemServicoResumo> buscarResumoPorId(@Param("id") UUID id);

    long countByStatus(StatusOS status);

    @Query("""
            SELECT new com.autopecas.autopecas.adapter.out.persistence.projection.ExecucaoConcluida(
                os.mecanicoResponsavelId, f.nome, os.dataInicioExecucao, os.dataFinalizacao)
            FROM OrdemServicoJpaEntity os
            JOIN FuncionarioJpaEntity f ON f.id = os.mecanicoResponsavelId
            WHERE os.status IN :status
            ORDER BY os.createdAt ASC
            """)
    List<ExecucaoConcluida> buscarExecucoesConcluidas(@Param("status") List<StatusOS> status);
}
