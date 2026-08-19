package com.autopecas.autopecas.adapter.out.persistence.mapper;

import com.autopecas.autopecas.adapter.out.persistence.entity.VeiculoJpaEntity;
import com.autopecas.autopecas.domain.model.veiculo.Veiculo;
import com.autopecas.autopecas.domain.vo.Placa;
import org.springframework.stereotype.Component;

/** Conversão entre o agregado Veiculo e sua entidade JPA. */
@Component
public class VeiculoJpaMapper {

    public Veiculo paraDominio(VeiculoJpaEntity entidade) {
        return Veiculo.reconstituir(entidade.getId(), new Placa(entidade.getPlaca()),
                entidade.getChassi(), entidade.getRenavam(), entidade.getMarca(), entidade.getModelo(),
                entidade.getAnoModelo(), entidade.getCor(), entidade.getObservacoes(),
                Boolean.TRUE.equals(entidade.getAtivo()), entidade.getClienteId());
    }

    /** Cria a entidade de um veículo ainda não persistido. */
    public VeiculoJpaEntity novaEntidade(Veiculo veiculo) {
        VeiculoJpaEntity entidade = new VeiculoJpaEntity();
        entidade.setPlaca(veiculo.getPlaca().valor());
        entidade.setClienteId(veiculo.getClienteId());
        aplicar(veiculo, entidade);
        return entidade;
    }

    /** Aplica o estado do agregado sobre uma entidade já carregada. A placa é imutável. */
    public void aplicar(Veiculo veiculo, VeiculoJpaEntity entidade) {
        entidade.setChassi(veiculo.getChassi());
        entidade.setRenavam(veiculo.getRenavam());
        entidade.setMarca(veiculo.getMarca());
        entidade.setModelo(veiculo.getModelo());
        entidade.setAnoModelo(veiculo.getAnoModelo());
        entidade.setCor(veiculo.getCor());
        entidade.setObservacoes(veiculo.getObservacoes());
        entidade.setAtivo(veiculo.isAtivo());
    }
}
