package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.domain.entity.ClientePF;
import com.autopecas.autopecas.domain.entity.ClientePJ;
import com.autopecas.autopecas.domain.enums.Genero;
import com.autopecas.autopecas.dto.cliente.ClienteCreatePFDTO;
import com.autopecas.autopecas.dto.cliente.ClienteCreatePJDTO;
import com.autopecas.autopecas.dto.cliente.ClienteResponseDTO;
import com.autopecas.autopecas.dto.cliente.ClienteUpdateDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.ClienteMapper;
import com.autopecas.autopecas.repository.ClientePFRepository;
import com.autopecas.autopecas.repository.ClientePJRepository;
import com.autopecas.autopecas.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public List<ClienteResponseDTO> listar(){
        return clienteRepository.findByAtivoTrue().stream()
                .map(clienteMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(UUID id){
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado através do ID: " + id));
        return clienteMapper.toResponse(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorDocumento(String documento){
        // busca por cpf ou cnpj
        Cliente cliente = clientePFRepository.findByCpf(documento)
                .map(c -> (Cliente) c)
                .or(() -> clientePJRepository.findByCnpj(documento)
                .map(c -> (Cliente) c))
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado, documento: " + documento));
        return clienteMapper.toResponse(cliente);
    }

    @Transactional
    public ClienteResponseDTO criarPF(ClienteCreatePFDTO dto){
        if (!dto.cpf().matches("\\d{11}")){
            throw new BusinessException("CPF inválido. Deve conter 11 dígitos numéricos.");
        }
        if (clientePFRepository.existsByCpf(dto.cpf())){
            throw new BusinessException("CPF já cadastrado: " + dto.cpf());
        }

        Genero genero = null;
        if (dto.genero() != null && !dto.genero().isBlank()){
            try{
                genero = Genero.valueOf(dto.genero().toUpperCase());
            } catch (IllegalArgumentException e){
                throw new BusinessException("Gênero inválido: " + dto.genero());
            }
        }

        ClientePF cliente = ClientePF.builder()
                .nome(dto.nome())
                .email(dto.email())
                .telefone(dto.telefone())
                .aceitaNotificacoes(dto.aceitaNotificacoes())
                .cpf(dto.cpf())
                .dataNascimento(dto.dataNascimento())
                .rg(dto.rg())
                .genero(genero)
                .build();

        ClientePF salvo = clientePFRepository.save(cliente);
        log.info("Cliente PF criado. ID {}", salvo.getId());
        return clienteMapper.toResponse(salvo);
    }

    @Transactional
    public ClienteResponseDTO criarPJ(ClienteCreatePJDTO dto){
        if (!dto.cnpj().matches("\\d{14}")){
            throw new BusinessException("CNPJ inválido. Deve conter 14 dígitos");
        }
        if (clientePJRepository.existsByCnpj(dto.cnpj())){
            throw new BusinessException("CNPJ já cadastrado: " + dto.cnpj());
        }

        ClientePJ cliente = ClientePJ.builder()
                .nome(dto.nome())
                .email(dto.email())
                .telefone(dto.telefone())
                .aceitaNotificacoes(dto.aceitaNotificacoes())
                .cnpj(dto.cnpj())
                .razaoSocial(dto.razaoSocial())
                .inscricaoEstadual(dto.inscricaoEstadual())
                .build();

        ClientePJ salvo = clientePJRepository.save(cliente);
        log.info("Cliente PJ criado. ID: {}", salvo.getId());
        return clienteMapper.toResponse(salvo);
    }

    @Transactional
    public ClienteResponseDTO atualizar(UUID id, ClienteUpdateDTO dto){
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado, id: " + id));

        if (dto.nome() != null && !dto.nome().isBlank()){
            cliente.setNome((dto.nome()));
        }
        if (dto.email() != null) {
            cliente.setEmail(dto.email());
        }
        if (dto.telefone() != null) {
            cliente.setTelefone(dto.telefone());
        }
        if (dto.aceitaNotificacoes() != null) {
            cliente.setAceitaNotificacoes(dto.aceitaNotificacoes());
        }

        Cliente atualizado = clienteRepository.save(cliente);
        log.info("Cliente atualizado. ID {}", atualizado.getId());
        return clienteMapper.toResponse(atualizado);
    }

    @Transactional
    public void desativar(UUID id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + id));
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
        log.info("Cliente desativado. ID: {}", id);
    }
}
