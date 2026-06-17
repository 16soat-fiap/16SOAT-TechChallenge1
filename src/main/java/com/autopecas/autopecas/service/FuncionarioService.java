package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Atendente;
import com.autopecas.autopecas.domain.entity.Funcionario;
import com.autopecas.autopecas.domain.entity.Mecanico;
import com.autopecas.autopecas.dto.funcionario.AtendenteCreateDTO;
import com.autopecas.autopecas.dto.funcionario.FuncionarioResponseDTO;
import com.autopecas.autopecas.dto.funcionario.MecanicoCreateDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.FuncionarioMapper;
import com.autopecas.autopecas.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final FuncionarioMapper funcionarioMapper;

    @Transactional(readOnly = true)
    public List<FuncionarioResponseDTO> listar() {
        return funcionarioRepository.findByAtivoTrue()
                .stream()
                .map(funcionarioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FuncionarioResponseDTO buscarPorId(UUID id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado, id: " + id));
        return funcionarioMapper.toResponse(funcionario);
    }

    @Transactional
    public FuncionarioResponseDTO criarMecanico(MecanicoCreateDTO dto) {
        validarCpfUnico(dto.cpf());

        Long seq = funcionarioRepository.proximoNumeroMecanico();
        String matricula = String.format("MEC-%04d", seq);

        Mecanico mecanico = Mecanico.builder()
                .matricula(matricula)
                .nome(dto.nome())
                .cpf(dto.cpf())
                .email(dto.email())
                .telefone(dto.telefone())
                .dataNascimento(dto.dataNascimento())
                .build();

        Mecanico salvo = funcionarioRepository.save(mecanico);
        log.info("Mecânico criado. ID: {}, Matrícula {}", salvo.getId(), salvo.getMatricula());
        return funcionarioMapper.toResponse(salvo);
    }

    @Transactional
    public FuncionarioResponseDTO criarAtendente(AtendenteCreateDTO dto) {
        validarCpfUnico(dto.cpf());

        Long seq = funcionarioRepository.proximoNumeroAtendente();
        String matricula = String.format("ATD-%04d", seq);

        Atendente atendente = Atendente.builder()
                .matricula(matricula)
                .cpf(dto.cpf())
                .nome(dto.nome())
                .email(dto.email())
                .telefone(dto.telefone())
                .dataNascimento(dto.dataNascimento())
                .build();

        Atendente salvo = funcionarioRepository.save(atendente);
        log.info("Atendente criado. ID: {}, Matrícula {}", salvo.getId(), salvo.getMatricula());
        return funcionarioMapper.toResponse(salvo);
    }

    @Transactional
    public void desativar(UUID id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado com ID: " + id));
        funcionario.setAtivo(false);
        funcionarioRepository.save(funcionario);
        log.info("Funcionário desativado. ID: {}", id);
    }

    private void validarCpfUnico(String cpf) {
        if (funcionarioRepository.existsByCpf(cpf)) {
            throw new BusinessException("CPF: " + cpf + " , já cadastrado");
        }
    }
}
