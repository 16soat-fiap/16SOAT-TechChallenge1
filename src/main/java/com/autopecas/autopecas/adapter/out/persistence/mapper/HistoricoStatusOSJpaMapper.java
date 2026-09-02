package com.autopecas.autopecas.adapter.out.persistence.mapper;

import com.autopecas.autopecas.adapter.out.persistence.entity.HistoricoStatusOSJpaEntity;
import com.autopecas.autopecas.domain.model.os.HistoricoStatusOS;
import org.springframework.stereotype.Component;

/** Conversão do histórico de status. Somente-inserção: não há método de atualização. */
@Component
public class HistoricoStatusOSJpaMapper {

    public HistoricoStatusOS paraDominio(HistoricoStatusOSJpaEntity entidade) {
        return HistoricoStatusOS.reconstituir(entidade.getId(), entidade.getOrdemServicoId(),
                entidade.getStatusAnterior(), entidade.getStatusNovo(), entidade.getObservacao(),
                entidade.getAlteradoPor(), entidade.getExecutadoPorId(), entidade.getCreatedAt());
    }

    public HistoricoStatusOSJpaEntity novaEntidade(HistoricoStatusOS historico) {
        HistoricoStatusOSJpaEntity entidade = new HistoricoStatusOSJpaEntity();
        entidade.setOrdemServicoId(historico.getOrdemServicoId());
        entidade.setStatusAnterior(historico.getStatusAnterior());
        entidade.setStatusNovo(historico.getStatusNovo());
        entidade.setObservacao(historico.getObservacao());
        entidade.setAlteradoPor(historico.getAlteradoPor());
        entidade.setExecutadoPorId(historico.getExecutadoPorId());
        return entidade;
    }
}
