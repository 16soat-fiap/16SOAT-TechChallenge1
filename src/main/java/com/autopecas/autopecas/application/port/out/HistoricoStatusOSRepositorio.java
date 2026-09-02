package com.autopecas.autopecas.application.port.out;

import com.autopecas.autopecas.domain.model.os.HistoricoStatusOS;

import java.util.List;
import java.util.UUID;

/** Port de saída do histórico de status, que é somente-inserção. */
public interface HistoricoStatusOSRepositorio {

    HistoricoStatusOS salvar(HistoricoStatusOS historico);

    List<HistoricoStatusOS> daOrdemServico(UUID ordemServicoId);
}
