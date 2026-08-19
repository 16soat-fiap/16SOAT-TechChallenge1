package com.autopecas.autopecas.application.port.out;

import com.autopecas.autopecas.domain.model.os.OrdemServico;

import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída do agregado OrdemServico.
 *
 * <p>Só expõe operações sobre o agregado. As listagens que precisam do nome do cliente e da
 * placa do veículo passam pela query port ConsultaOrdemServico.
 */
public interface OrdemServicoRepositorio {

    OrdemServico salvar(OrdemServico ordemServico);

    Optional<OrdemServico> porId(UUID id);

    Optional<OrdemServico> porNumero(String numero);

    boolean existePorId(UUID id);
}
