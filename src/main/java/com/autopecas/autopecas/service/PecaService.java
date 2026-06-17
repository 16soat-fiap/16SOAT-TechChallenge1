package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.MovimentacaoEstoque;
import com.autopecas.autopecas.domain.entity.Peca;
import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.dto.peca.MovimentacaoCreateDTO;
import com.autopecas.autopecas.dto.peca.MovimentacaoResponseDTO;
import com.autopecas.autopecas.dto.peca.PecaCreateDTO;
import com.autopecas.autopecas.dto.peca.PecaResponseDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.PecaMapper;
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
    private final EstoqueService estoqueService;
    private final PecaMapper pecaMapper;

    @Transactional(readOnly = true)
    public List<PecaResponseDTO> listar(Boolean estoqueBaixo) {
        if (Boolean.TRUE.equals(estoqueBaixo)) {
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

    @Transactional(readOnly = true)
    public PecaResponseDTO buscarPorId(UUID id) {
        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Id " + id + " não encontrado"));
        return pecaMapper.toResponse(peca);
    }

    @Transactional(readOnly = true)
    public PecaResponseDTO buscarPorCodigo(String codigo) {
        Peca peca = pecaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Peca " + codigo + " não encontrada"));
        return pecaMapper.toResponse(peca);
    }

    @Transactional
    public PecaResponseDTO criar(PecaCreateDTO pecaDto) {
        if (pecaRepository.existsByCodigo(pecaDto.codigo())) {
            throw new BusinessException("Código da peça já está cadastrado: " + pecaDto.codigo());
        }

        int quantidadeInicial = pecaDto.quantidadeInicial() != null ? pecaDto.quantidadeInicial() : 0;
        int quantidadeMinima  = pecaDto.quantidadeMinima()  != null ? pecaDto.quantidadeMinima()  : 1;
        String unidade        = pecaDto.unidade()           != null ? pecaDto.unidade()           : "un";

        Peca peca = Peca.builder()
                .codigo(pecaDto.codigo())
                .nome(pecaDto.nome())
                .descricao(pecaDto.descricao())
                .precoVenda(pecaDto.precoVenda())
                .quantidadeMinima(quantidadeMinima)
                .unidade(unidade)
                .build();

        Peca salva = pecaRepository.save(peca);
        log.info("Peça criada. ID: {}, Código: {}", salva.getId(), salva.getCodigo());

        if (quantidadeInicial > 0) {
            estoqueService.registrarMovimentacao(
                    salva,
                    TipoMovimentacaoEstoque.ENTRADA,
                    quantidadeInicial,
                    "Estoque inicial na criação da peça",
                    null,
                    null
            );
            log.info("Estoque inicial registrado. Peça: {}, Quantidade: {}", salva.getCodigo(), quantidadeInicial);
        }

        return pecaMapper.toResponse(salva);
    }

    @Transactional
    public PecaResponseDTO atualizar(UUID id, PecaCreateDTO dto) {
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

    @Transactional
    public MovimentacaoResponseDTO registrarMovimentacao(UUID id, MovimentacaoCreateDTO dto) {
        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada. ID: " + id));

        TipoMovimentacaoEstoque tipo;
        try {
            tipo = TipoMovimentacaoEstoque.valueOf(dto.tipo().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Tipo de movimentação inválido: " + dto.tipo());
        }

        MovimentacaoEstoque movimentacao = estoqueService.registrarMovimentacao(
                peca, tipo, dto.quantidade(), dto.motivo(), null, null);

        log.info("Movimentação registrada manualmente. Peça: {}, Tipo: {}, Qtd: {}", peca.getCodigo(), tipo, dto.quantidade());
        return new MovimentacaoResponseDTO(
                movimentacao.getId(),
                movimentacao.getTipo().name(),
                movimentacao.getQuantidade(),
                movimentacao.getSaldoApos(),
                movimentacao.getMotivo(),
                movimentacao.getCreatedAt()
        );
    }
}
