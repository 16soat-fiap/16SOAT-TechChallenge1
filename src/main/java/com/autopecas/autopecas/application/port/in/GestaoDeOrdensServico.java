package com.autopecas.autopecas.application.port.in;

import com.autopecas.autopecas.application.pagination.Pagina;
import com.autopecas.autopecas.application.pagination.PaginaRequisicao;
import com.autopecas.autopecas.application.port.in.view.OrdemServicoView;
import com.autopecas.autopecas.domain.enums.StatusOS;

import java.util.List;
import java.util.UUID;

/** Inbound port do agregado OrdemServico. */
public interface GestaoDeOrdensServico {

    /**
     * Fila de trabalho da oficina.
     *
     * <p>Sem filtro explícito de status, exclui as OS já encerradas (FINALIZADA e ENTREGUE) e
     * ordena por urgência — EM_EXECUCAO, AGUARDANDO_APROVACAO, EM_DIAGNOSTICO, RECEBIDA — com as
     * mais antigas primeiro dentro de cada faixa. Informar {@code status} é opt-in explícito e
     * devolve exatamente aquele status, encerrado ou não.
     */
    Pagina<OrdemServicoView> listar(StatusOS status, UUID clienteId, UUID mecanicoId,
                                    PaginaRequisicao paginacao);

    OrdemServicoView porNumero(String numero);

    OrdemServicoView abrir(Abrir comando);

    OrdemServicoView avancarStatus(UUID id, AvancarStatus comando);

    OrdemServicoView registrarDiagnostico(UUID id, String diagnostico);

    OrdemServicoView atribuirMecanico(UUID id, UUID mecanicoId);

    /**
     * emailAtendente identifica quem recepcionou o veículo; vem do token e pode ser nulo,
     * caso em que o histórico é registrado como SISTEMA.
     *
     * <p>Serviços e peças são opcionais na abertura: registram o que já se sabe que será feito,
     * com o preço vigente no catálogo no momento da recepção. Eles <b>não</b> baixam estoque —
     * a baixa continua acontecendo na aprovação do orçamento, que é o momento em que a peça é
     * de fato comprometida. Lançar a peça aqui e de novo na aprovação contaria a saída duas vezes.
     */
    record Abrir(
            UUID clienteId,
            UUID veiculoId,
            String queixaCliente,
            String observacoesEntrada,
            Integer quilometragemEntrada,
            String emailAtendente,
            List<ItemServico> itensServico,
            List<ItemPeca> itensPeca
    ) {
        /** Quantidade nula assume 1, mesma convenção do orçamento. */
        public record ItemServico(UUID servicoId, Integer quantidade) {
        }

        public record ItemPeca(UUID pecaId, Integer quantidade) {
        }
    }

    /** novoStatus chega como texto e é convertido no caso de uso. */
    record AvancarStatus(
            String novoStatus,
            String observacao,
            String emailFuncionario
    ) {
    }
}
