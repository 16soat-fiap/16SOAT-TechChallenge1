package com.autopecas.autopecas.domain.enums;

import com.autopecas.autopecas.adapter.out.persistence.repository.OrdemServicoJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A fila de trabalho da oficina, verificada nos dois lugares onde ela existe.
 *
 * <p>A regra é declarada no enum e materializada num CASE dentro do JPQL. O último teste compara
 * os dois lados: se alguém mudar a prioridade no enum e esquecer a query — ou o contrário — a
 * build quebra em vez de a listagem passar a mentir silenciosamente.
 */
@DisplayName("StatusOS — fila de trabalho")
class StatusOSFilaTest {

    @Test
    @DisplayName("a ordem é EM_EXECUCAO, AGUARDANDO_APROVACAO, EM_DIAGNOSTICO, RECEBIDA")
    void ordemDaFila() {
        assertThat(StatusOS.emAndamento()).containsExactly(
                StatusOS.EM_EXECUCAO,
                StatusOS.AGUARDANDO_APROVACAO,
                StatusOS.EM_DIAGNOSTICO,
                StatusOS.RECEBIDA);
    }

    @Test
    @DisplayName("FINALIZADA e ENTREGUE ficam fora da fila")
    void encerradasForaDaFila() {
        assertThat(StatusOS.FINALIZADA.encerrouAtendimento()).isTrue();
        assertThat(StatusOS.ENTREGUE.encerrouAtendimento()).isTrue();
        assertThat(StatusOS.emAndamento())
                .doesNotContain(StatusOS.FINALIZADA, StatusOS.ENTREGUE);
    }

    @Test
    @DisplayName("as encerradas têm prioridade pior que qualquer status em andamento")
    void encerradasTemPrioridadePior() {
        int piorEmAndamento = StatusOS.emAndamento().stream()
                .mapToInt(StatusOS::prioridadeNaFila)
                .max()
                .orElseThrow();

        assertThat(StatusOS.FINALIZADA.prioridadeNaFila()).isGreaterThan(piorEmAndamento);
        assertThat(StatusOS.ENTREGUE.prioridadeNaFila()).isGreaterThan(piorEmAndamento);
    }

    @Test
    @DisplayName("o ORDER BY do repositório reproduz a prioridade declarada no enum")
    void jpqlEspelhaOEnum() {
        String orderBy = OrdemServicoJpaRepository.ORDER_BY_FILA;

        for (StatusOS status : StatusOS.values()) {
            if (status.encerrouAtendimento()) {
                continue;
            }
            // "WHEN ...StatusOS.EM_EXECUCAO THEN 1"
            assertThat(orderBy)
                    .as("o CASE do JPQL precisa mapear %s para a prioridade %d declarada no enum",
                            status, status.prioridadeNaFila())
                    .containsPattern("StatusOS\\." + status.name()
                            + "\\s+THEN\\s+" + status.prioridadeNaFila());
        }

        assertThat(orderBy)
                .as("dentro de cada faixa de prioridade, as mais antigas vêm primeiro")
                .contains("os.createdAt ASC");
    }

    @Test
    @DisplayName("o recorte da fila é feito pelos status em andamento")
    void whereUsaStatusEmAndamento() {
        assertThat(OrdemServicoJpaRepository.WHERE_EM_ANDAMENTO)
                .contains("os.status IN :statusEmAndamento");
        assertThat(List.of(StatusOS.values())).hasSize(StatusOS.emAndamento().size() + 2);
    }
}
