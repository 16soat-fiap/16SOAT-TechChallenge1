package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.domain.entity.Veiculo;
import com.autopecas.autopecas.dto.veiculo.VeiculoCreateDTO;
import com.autopecas.autopecas.dto.veiculo.VeiculoResponseDTO;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.VeiculoMapper;
import com.autopecas.autopecas.repository.ClienteRepository;
import com.autopecas.autopecas.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoMapper veiculoMapper;

    @Transactional(readOnly = true)
    public List<VeiculoResponseDTO> listar(){
        return veiculoRepository.findAll()
                .stream()
                .map(veiculoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VeiculoResponseDTO buscarPorId(UUID id){
        Veiculo veiculo = veiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veiculo não encontrado, id " + id));
        return veiculoMapper.toResponse(veiculo);
    }

    @Transactional(readOnly = true)
    public VeiculoResponseDTO buscarPorPlaca(String placa){
        Veiculo veiculo = veiculoRepository.findByPlaca(placa.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Placa " + placa + " Não encontrada"));
        return veiculoMapper.toResponse(veiculo);
    }

    @Transactional(readOnly = true)
    public List<VeiculoResponseDTO> listarPorCliente(UUID clienteId){
        if(!clienteRepository.existsById(clienteId)){
            throw new ResourceNotFoundException("Cliente não encontrado, id: " + clienteId);
        }
        return veiculoRepository.findByClienteId(clienteId)
                .stream()
                .map(veiculoMapper::toResponse)
                .toList();
    }

    @Transactional
    public VeiculoResponseDTO criar(VeiculoCreateDTO dto){
        String placaFormatada = dto.placa().toUpperCase();
        if(veiculoRepository.existsByPlaca(placaFormatada)){
            throw new ResourceNotFoundException("Placa " + placaFormatada + " já cadastrada");
        }

        Cliente cliente = clienteRepository.findById(dto.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado, id: " + dto.clienteId()));

        Veiculo veiculo = Veiculo.builder()
                .placa(placaFormatada)
                .chassi(dto.chassi())
                .renavam(dto.renavam())
                .marca(dto.marca())
                .modelo(dto.modelo())
                .anoModelo(dto.anoModelo())
                .cor(dto.cor())
                .cliente(cliente)
                .build();

        Veiculo salvo = veiculoRepository.save(veiculo);
        log.info("Veículo criado. ID: {}, Placa: {}", salvo.getId(), salvo.getPlaca());
        return veiculoMapper.toResponse(salvo);
    }

    @Transactional
    public VeiculoResponseDTO atualizar(UUID id, VeiculoCreateDTO dto) {
        Veiculo veiculo = veiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo " +id+ " não encontrado com ID: "));

        if (dto.marca() != null && !dto.marca().isBlank()) {
            veiculo.setMarca(dto.marca());
        }
        if (dto.modelo() != null && !dto.modelo().isBlank()) {
            veiculo.setModelo(dto.modelo());
        }
        if (dto.anoModelo() != null) {
            veiculo.setAnoModelo(dto.anoModelo());
        }
        if (dto.cor() != null) {
            veiculo.setCor(dto.cor());
        }

        Veiculo atualizado = veiculoRepository.save(veiculo);
        log.info("Veículo atualizado. ID: {}", atualizado.getId());
        return veiculoMapper.toResponse(atualizado);
    }

    @Transactional
    public void deletar(UUID id) {
        if (!veiculoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Veículo não encontrado, id: " + id);
        }
        veiculoRepository.deleteById(id);
        log.info("Veículo deletado. ID: {}", id);
    }
}
