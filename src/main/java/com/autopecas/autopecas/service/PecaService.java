package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Peca;
import com.autopecas.autopecas.dto.peca.PecaResponseDTO;
import com.autopecas.autopecas.dto.peca.PecaCreateDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.PecaMapper;
import com.autopecas.autopecas.repository.MovimentacaoEstoqueRepository;
import com.autopecas.autopecas.repository.PecaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PecaService {

    private final PecaRepository pecaRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final PecaMapper pecaMapper;

    @Transactional(readOnly = true)
    public List<PecaResponseDTO> listar(Boolean estoqueBaixo){
        if(Boolean.TRUE.equals(estoqueBaixo)){
            return pecaRepository.findEstoqueBaixo()
                    .stream()
                    .map(pecaMapper::toResponse)
                    .toList();
        }

        return pecaRepository.findByAtivoTrue()
                .stream()
                .map(pecaMapper::toResponse)
                .toList();
    }

    @Transactional
    public PecaResponseDTO buscarPorId(UUID id){
        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("Id "+id+" não encontrado"));
        return pecaMapper.toResponse(peca);
    }

    @Transactional
    public PecaResponseDTO buscarPorCodigo(String codigo){
        Peca peca = pecaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Peca "+ codigo+" não encontrada"));
        return pecaMapper.toResponse(peca);
    }

    @Transactional
    public PecaResponseDTO criar(PecaCreateDTO pecaDto){
        if(pecaRepository.existsByCodigo(pecaDto.codigo())){
            throw new BusinessException("Código da peça já está cadastrado: "+pecaDto.codigo());
        }

        int quantidadeInicial = pecaDto.quantidadeInicial() != null ? pecaDto.quantidadeInicial() : 0;
        int quantidadeMinima = pecaDto.quantidadeMinima() != null ? pecaDto.quantidadeMinima() : 1;
        String unidade = pecaDto.unidade() != null ? pecaDto.unidade() : "un";

        Peca peca = Peca.builder()
                .codigo(pecaDto.codigo())
                .nome(pecaDto.nome())
                .descricao(pecaDto.descricao())
                .precoVenda(pecaDto.precoVenda())
                .quantidadeMinima(quantidadeMinima)
                .quantidadeEstoque(quantidadeInicial)
                .unidade(unidade)
                .build();

        Peca salva = pecaRepository.save(peca);
        log.info("Peça criada. ID: {}, Código: {}", salva.getId(), salva.getCodigo());
        return pecaMapper.toResponse(salva);
    }

    @Transactional
    public PecaResponseDTO atualizar(UUID id, PecaCreateDTO dto){
        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada"));

        if (dto.nome() != null && !dto.nome().isBlank()) {
            peca.setNome(dto.nome());
        }
        if (dto.descricao() != null) {
            peca.setDescricao(dto.descricao());
        }
        if (dto.precoVenda() != null) {
            peca.setPrecoVenda(dto.precoVenda());
        }
        if (dto.quantidadeMinima() != null) {
            peca.setQuantidadeMinima(dto.quantidadeMinima());
        }
        if (dto.unidade() != null && !dto.unidade().isBlank()) {
            peca.setUnidade(dto.unidade());
        }
        Peca atualizada = pecaRepository.save(peca);
        log.info("Peça atualizada. ID: {}", atualizada.getId());
        return pecaMapper.toResponse(atualizada);
    }

    @Transactional
    public void desativar(UUID id) {
        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada. ID: " + id));
        peca.setAtivo(false);
        pecaRepository.save(peca);
        log.info("Peça desativada. ID: {}", id);
    }
}
