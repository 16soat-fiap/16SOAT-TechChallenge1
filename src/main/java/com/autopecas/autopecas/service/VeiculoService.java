package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.domain.entity.Veiculo;
import com.autopecas.autopecas.dto.veiculo.VeiculoCreateDTO;
import com.autopecas.autopecas.dto.veiculo.VeiculoResponseDTO;
import com.autopecas.autopecas.dto.veiculo.VeiculoUpdateDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.VeiculoMapper;
import com.autopecas.autopecas.repository.ClienteRepository;
import com.autopecas.autopecas.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoMapper veiculoMapper;

    @Transactional(readOnly = true)
    public List<VeiculoResponseDTO> listar() {
        return veiculoRepository.findAllByAtivoTrue()
                .stream()
                .map(veiculoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VeiculoResponseDTO buscarPorId(UUID id) {
        Veiculo veiculo = veiculoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veiculo não encontrado, id " + id));
        return veiculoMapper.toResponse(veiculo);
    }

    @Transactional(readOnly = true)
    public VeiculoResponseDTO buscarPorPlaca(String placa) {
        String placaFormatada = formatarPlaca(placa);
        Veiculo veiculo = veiculoRepository.findByPlacaAndAtivoTrue(placaFormatada)
                .orElseThrow(() -> new ResourceNotFoundException("Placa " + placa + " Não encontrada"));
        return veiculoMapper.toResponse(veiculo);
    }

    @Transactional(readOnly = true)
    public List<VeiculoResponseDTO> listarPorCliente(UUID clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ResourceNotFoundException("Cliente não encontrado, id: " + clienteId);
        }
        return veiculoRepository.findByClienteIdAndAtivoTrue(clienteId)
                .stream()
                .map(veiculoMapper::toResponse)
                .toList();
    }

    @Transactional
    public VeiculoResponseDTO criar(VeiculoCreateDTO dto) {

        String placaFormatada  = validarPlaca(dto.placa());

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
    public VeiculoResponseDTO atualizar(UUID id, VeiculoUpdateDTO dto) {
        Veiculo veiculo = veiculoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado, id: " + id));

        if (dto.marca() != null) {
            veiculo.setMarca(dto.marca());
        }

        if (dto.modelo() != null) {
            veiculo.setModelo(dto.modelo());
        }

        if (dto.anoModelo() != null) {
            veiculo.setAnoModelo(dto.anoModelo());
        }
        if (dto.cor() != null) {
            veiculo.setCor(dto.cor());
        }
        if (dto.chassi() != null) {
            validarChassiDuplicado(dto.chassi(), id);
            veiculo.setChassi(dto.chassi());
        }
        if (dto.renavam() != null) {
            validarRenavamDuplicado(dto.renavam(), id);
            veiculo.setRenavam(dto.renavam());
        }


        Veiculo atualizado = veiculoRepository.save(veiculo);
        log.info("Veículo atualizado. ID: {}, Placa: {}", atualizado.getId(), atualizado.getPlaca());
        return veiculoMapper.toResponse(atualizado);
    }

    private String validarPlaca(String placa) {
        if (placa == null || placa.trim().isEmpty()) {
            throw new BusinessException("A placa não pode estar vazia.");
        }

        String placaLimpa = formatarPlaca(placa);

        String placaAntiga = "^[A-Z]{3}[0-9]{4}$";
        String placaMercosul = "^[A-Z]{3}[0-9][A-Z][0-9]{2}$";

        if (!placaLimpa.matches(placaAntiga) && !placaLimpa.matches(placaMercosul)) {
            throw new BusinessException("Placa de veículo com formato inválido.");
        }

        if (veiculoRepository.existsByPlaca(placaLimpa)) {
            throw new BusinessException("Placa " + placaLimpa + " já cadastrada");
        }

        return placaLimpa;
    }

    private String formatarPlaca(String placa){
        return placa.replace("-", "").trim().toUpperCase();
    }

    private void validarRenavamDuplicado(String renavam, UUID id) {
        veiculoRepository.findByRenavam(renavam)
                .filter(v -> !v.getId().equals(id))
                .ifPresent(v -> { throw new BusinessException("RENAVAM já cadastrado."); });
    }

    private void validarChassiDuplicado(String chassi, UUID veiculoId) {
        Optional<Veiculo> veiculoExistente = veiculoRepository.findByChassi(chassi);
        if (veiculoExistente.isPresent() && !veiculoExistente.get().getId().equals(veiculoId)) {
            throw new BusinessException("Chassi já cadastrado.");
        }
    }

    @Transactional
    public void deletar(UUID id) {
        Veiculo veiculo = veiculoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado, id: " + id));
        veiculo.setAtivo(false);
        veiculoRepository.save(veiculo);
        log.info("Veículo desativado (soft delete). ID: {}", id);
    }
}
