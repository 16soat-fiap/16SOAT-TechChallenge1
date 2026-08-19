package com.autopecas.autopecas.application.port.in;

import com.autopecas.autopecas.application.pagination.Pagina;
import com.autopecas.autopecas.application.pagination.PaginaRequisicao;
import com.autopecas.autopecas.application.port.in.view.OrdemServicoView;
import com.autopecas.autopecas.domain.enums.StatusOS;

import java.util.UUID;

/** Inbound port do agregado OrdemServico. */
public interface GestaoDeOrdensServico {

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
     */
    record Abrir(
            UUID clienteId,
            UUID veiculoId,
            String queixaCliente,
            String observacoesEntrada,
            Integer quilometragemEntrada,
            String emailAtendente
    ) {
    }

    /** novoStatus chega como texto e é convertido no caso de uso. */
    record AvancarStatus(
            String novoStatus,
            String observacao,
            String emailFuncionario
    ) {
    }
}
