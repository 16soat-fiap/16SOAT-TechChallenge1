package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.domain.entity.Peca;
import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.dto.peca.MovimentacaoCreateDTO;
import com.autopecas.autopecas.dto.peca.MovimentacaoResponseDTO;
import com.autopecas.autopecas.dto.peca.PecaCreateDTO;
import com.autopecas.autopecas.dto.peca.PecaResponseDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.repository.PecaRepository;
import com.autopecas.autopecas.service.EstoqueService;
import com.autopecas.autopecas.service.PecaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pecas")
@RequiredArgsConstructor
public class PecaController {

    private final PecaService pecaService;
    private final EstoqueService estoqueService;
    private final PecaRepository pecaRepository;

    @GetMapping
    public ResponseEntity<List<PecaResponseDTO>> listar(@RequestParam(required = false) Boolean estoqueBaixo){
        return ResponseEntity.ok(pecaService.listar(estoqueBaixo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PecaResponseDTO> buscarPorId(@PathVariable UUID id){
        return ResponseEntity.ok(pecaService.buscarPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<PecaResponseDTO> buscarPorCodigo(@RequestParam String codigo) {
        return ResponseEntity.ok(pecaService.buscarPorCodigo(codigo));
    }

    @PostMapping
    public ResponseEntity<PecaResponseDTO> criar(@Valid @RequestBody PecaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pecaService.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PecaResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PecaCreateDTO dto) {
        return ResponseEntity.ok(pecaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        pecaService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/movimentacoes")
    public ResponseEntity<MovimentacaoResponseDTO> registrarMovimentacao(
            @PathVariable UUID id,
            @Valid @RequestBody MovimentacaoCreateDTO dto) {

        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Peça"+id+"não encontrada"));

        TipoMovimentacaoEstoque tipo;
        try {
            tipo = TipoMovimentacaoEstoque.valueOf(dto.tipo().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Tipo de movimentação inválido: " + dto.tipo());
        }

        var movimentacao = estoqueService.registrarMovimentacao(peca, tipo, dto.quantidade(), dto.motivo(), null, null);

        MovimentacaoResponseDTO response = new MovimentacaoResponseDTO(
                movimentacao.getId(),
                movimentacao.getTipo().name(),
                movimentacao.getQuantidade(),
                movimentacao.getSaldoApos(),
                movimentacao.getMotivo(),
                movimentacao.getCreatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
