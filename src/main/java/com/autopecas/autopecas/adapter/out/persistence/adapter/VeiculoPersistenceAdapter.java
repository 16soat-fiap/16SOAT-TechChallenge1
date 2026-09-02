package com.autopecas.autopecas.adapter.out.persistence.adapter;

import com.autopecas.autopecas.adapter.out.persistence.entity.VeiculoJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.mapper.VeiculoJpaMapper;
import com.autopecas.autopecas.adapter.out.persistence.repository.VeiculoJpaRepository;
import com.autopecas.autopecas.application.port.out.VeiculoRepositorio;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.veiculo.Veiculo;
import com.autopecas.autopecas.domain.vo.Placa;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter de persistência do agregado Veiculo. */
@Component
public class VeiculoPersistenceAdapter implements VeiculoRepositorio {

    private final VeiculoJpaRepository repository;
    private final VeiculoJpaMapper mapper;

    public VeiculoPersistenceAdapter(VeiculoJpaRepository repository, VeiculoJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Veiculo salvar(Veiculo veiculo) {
        VeiculoJpaEntity entidade;
        if (veiculo.isNovo()) {
            entidade = mapper.novaEntidade(veiculo);
        } else {
            entidade = repository.findById(veiculo.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Veículo não encontrado, id: " + veiculo.getId()));
            mapper.aplicar(veiculo, entidade);
        }
        return mapper.paraDominio(repository.save(entidade));
    }

    @Override
    public Optional<Veiculo> porId(UUID id) {
        return repository.findById(id).map(mapper::paraDominio);
    }

    @Override
    public Optional<Veiculo> porIdAtivo(UUID id) {
        return repository.findByIdAndAtivoTrue(id).map(mapper::paraDominio);
    }

    @Override
    public Optional<Veiculo> porPlacaAtiva(Placa placa) {
        return repository.findByPlacaAndAtivoTrue(placa.valor()).map(mapper::paraDominio);
    }

    @Override
    public Optional<Veiculo> porChassi(String chassi) {
        return repository.findByChassi(chassi).map(mapper::paraDominio);
    }

    @Override
    public Optional<Veiculo> porRenavam(String renavam) {
        return repository.findByRenavam(renavam).map(mapper::paraDominio);
    }

    @Override
    public List<Veiculo> ativos() {
        return repository.findAllByAtivoTrue().stream().map(mapper::paraDominio).toList();
    }

    @Override
    public List<Veiculo> ativosDoCliente(UUID clienteId) {
        return repository.findByClienteIdAndAtivoTrue(clienteId).stream()
                .map(mapper::paraDominio).toList();
    }
}
