package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Servico;
import com.autopecas.autopecas.dto.servico.ServicoCreateDTO;
import com.autopecas.autopecas.dto.servico.ServicoResponseDTO;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.ServicoMapper;
import com.autopecas.autopecas.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final ServicoMapper servicoMapper;

    @Transactional(readOnly = true)
    public List<ServicoResponseDTO> listar(){
        return servicoRepository.findByAtivoTrue()
                .stream()
                .map(servicoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServicoResponseDTO buscarPorId(UUID id){
            Servico servico = servicoRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Servico " +id+ " não encontrado"));
            return servicoMapper.toResponse(servico);
    }

    @Transactional
    public ServicoResponseDTO criar(ServicoCreateDTO dto) {
        Servico servico = servicoMapper.toEntity(dto);
        Servico salvo = servicoRepository.save(servico);
        log.info("Serviço criado. ID: {}, Nome: {}", salvo.getId(), salvo.getNome());
        return servicoMapper.toResponse(salvo);
    }

    @Transactional
    public ServicoResponseDTO atualizar(UUID id, ServicoCreateDTO dto) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado, ID: " + id));

        servico.setNome(dto.nome());
        if (dto.descricao() != null) {
            servico.setDescricao(dto.descricao());
        }
        servico.setPrecoBase(dto.precoBase());
        if (dto.tempoEstimadoMinutos() != null) {
            servico.setTempoEstimadoMinutos(dto.tempoEstimadoMinutos());
        }

        Servico atualizado = servicoRepository.save(servico);
        log.info("Serviço atualizado. ID: {}", atualizado.getId());
        return servicoMapper.toResponse(atualizado);
    }

    @Transactional
    public void desativar(UUID id) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço " +id+" não encontrado"));
        servico.setAtivo(false);
        servicoRepository.save(servico);
        log.info("Serviço desativado. ID: {}", id);
    }
}
