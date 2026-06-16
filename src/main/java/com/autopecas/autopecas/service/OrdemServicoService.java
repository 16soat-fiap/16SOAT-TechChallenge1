package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.*;
import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.dto.os.AtribuirMecanicoDTO;
import com.autopecas.autopecas.dto.os.AvancarStatusDTO;
import com.autopecas.autopecas.dto.os.OrdemServicoCreateDTO;
import com.autopecas.autopecas.dto.os.OrdemServicoResponseDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.OrdemServicoMapper;
import com.autopecas.autopecas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrdemServicoService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final HistoricoStatusOsRepository historicoStatusOsRepository;
    private final OrdemServicoMapper ordemServicoMapper;


    @Transactional(readOnly = true)
    public Page<OrdemServicoResponseDTO> listar(StatusOS status, UUID clienteId, UUID mecanicoId, Pageable pageable) {
        if (status != null) {
            return ordemServicoRepository.findByStatus(status, pageable)
                    .map(ordemServicoMapper::toResponse);
        }
        if (clienteId != null) {
            return ordemServicoRepository.findByClienteId(clienteId, pageable)
                    .map(ordemServicoMapper::toResponse);
        }
        if (mecanicoId != null) {
            return ordemServicoRepository.findByMecanicoResponsavelId(mecanicoId, pageable)
                    .map(ordemServicoMapper::toResponse);
        }
        return ordemServicoRepository.findAll(pageable)
                .map(ordemServicoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrdemServicoResponseDTO buscarPorNumero(String numero) {
        OrdemServico os = ordemServicoRepository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada com número: " + numero));
        return ordemServicoMapper.toResponse(os);
    }

    @Transactional
    public OrdemServicoResponseDTO criar(OrdemServicoCreateDTO dto, String emailAtendente) {
        Cliente cliente = clienteRepository.findById(dto.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + dto.clienteId()));

        Veiculo veiculo = veiculoRepository.findByIdAndAtivoTrue(dto.veiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Veículo inativo ou não encontrado com ID: " + dto.veiculoId()));

        Long nextVal = ordemServicoRepository.proximoNumero();
        String numero = "OS-" + String.format("%06d", nextVal);

        OrdemServico os = OrdemServico.builder()
                .numero(numero)
                .status(StatusOS.RECEBIDA)
                .cliente(cliente)
                .veiculo(veiculo)
                .queixaCliente(dto.queixaCliente())
                .observacoesEntrada(dto.observacoesEntrada())
                .build();

        // Associar atendente se houver
        Atendente atendente = null;
        if (emailAtendente != null) {
            Funcionario func = funcionarioRepository.findAll().stream()
                    .filter(f -> emailAtendente.equals(f.getEmail()) && f instanceof Atendente)
                    .findFirst()
                    .orElse(null);
            if (func instanceof Atendente a) {
                atendente = a;
                os.setAtendenteRecepcao(atendente);
            }
        }

        OrdemServico salva = ordemServicoRepository.save(os);

        // Registrar abertura no histórico
        HistoricoStatusOS historico;
        if (atendente != null) {
            historico = HistoricoStatusOS.abertura(salva, atendente);
        } else {
            historico = HistoricoStatusOS.porSistema(salva, null, StatusOS.RECEBIDA, "Ordem de serviço aberta.");
        }
        historicoStatusOsRepository.save(historico);

        log.info("Ordem de serviço criada. Número: {}", salva.getNumero());
        return ordemServicoMapper.toResponse(salva);
    }

    @Transactional
    public OrdemServicoResponseDTO avancarStatus(UUID id, AvancarStatusDTO dto, String emailFuncionario) {
        OrdemServico os = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada com ID: " + id));

        StatusOS novoStatus;
        try {
            novoStatus = StatusOS.valueOf(dto.novoStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Status inválido: " + dto.novoStatus());
        }

        StatusOS statusAnterior = os.getStatus();

        try {
            os.avancarStatus(novoStatus);
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }

        OrdemServico atualizada = ordemServicoRepository.save(os);

        // Registrar no histórico
        Funcionario funcionario = null;
        if (emailFuncionario != null) {
            funcionario = funcionarioRepository.findAll().stream()
                    .filter(f -> emailFuncionario.equals(f.getEmail()))
                    .findFirst()
                    .orElse(null);
        }

        HistoricoStatusOS historico;
        if (funcionario != null) {
            historico = HistoricoStatusOS.porFuncionario(atualizada, statusAnterior, novoStatus,
                    dto.observacao(), funcionario);
        } else {
            historico = HistoricoStatusOS.porSistema(atualizada, statusAnterior, novoStatus, dto.observacao());
        }
        historicoStatusOsRepository.save(historico);

        log.info("Status da OS {} avançado de {} para {}. Número: {}", id, statusAnterior, novoStatus, atualizada.getNumero());
        return ordemServicoMapper.toResponse(atualizada);
    }

    @Transactional
    public OrdemServicoResponseDTO atribuirMecanico(UUID id, AtribuirMecanicoDTO dto) {
        OrdemServico os = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada com ID: " + id));

        Funcionario funcionario = funcionarioRepository.findById(dto.mecanicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Mecânico não encontrado com ID: " + dto.mecanicoId()));

        if (!(funcionario instanceof Mecanico mecanico)) {
            throw new BusinessException("O funcionário informado não é um mecânico.");
        }

        os.setMecanicoResponsavel(mecanico);
        OrdemServico atualizada = ordemServicoRepository.save(os);
        log.info("Mecânico {} atribuído à OS {}. Número: {}", dto.mecanicoId(), id, atualizada.getNumero());
        return ordemServicoMapper.toResponse(atualizada);
    }
}
