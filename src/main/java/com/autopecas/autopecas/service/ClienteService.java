package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.dto.cliente.ClienteResponseDTO;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.ClienteMapper;
import com.autopecas.autopecas.repository.ClientePFRepository;
import com.autopecas.autopecas.repository.ClientePJRepository;
import com.autopecas.autopecas.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClientePFRepository clientePFRepository;
    private final ClientePJRepository clientePJRepository;
    private final ClienteMapper clienteMapper;

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(UUID id){
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado através do ID: " + id));
        return clienteMapper.toResponse(cliente);
    }

}
