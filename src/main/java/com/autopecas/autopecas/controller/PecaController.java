package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.dto.peca.MovimentacaoCreateDTO;
import com.autopecas.autopecas.dto.peca.MovimentacaoResponseDTO;
import com.autopecas.autopecas.dto.peca.PecaCreateDTO;
import com.autopecas.autopecas.dto.peca.PecaResponseDTO;
import com.autopecas.autopecas.service.PecaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pecas")
@RequiredArgsConstructor
public class PecaController {

    private final PecaService pecaService;

    // MECANICO consulta peças disponíveis para incluir em orçamentos/OS
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<List<PecaResponseDTO>> listar(
            @RequestParam(required = false) Boolean estoqueBaixo) {
        return ResponseEntity.ok(pecaService.listar(estoqueBaixo));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<PecaResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(pecaService.buscarPorId(id));
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<PecaResponseDTO> buscarPorCodigo(@RequestParam String codigo) {
        return ResponseEntity.ok(pecaService.buscarPorCodigo(codigo));
    }

    // Cadastro e edição de peças — somente ADMIN
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PecaResponseDTO> criar(@Valid @RequestBody PecaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pecaService.criar(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PecaResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PecaCreateDTO dto) {
        return ResponseEntity.ok(pecaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        pecaService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    // Movimentação de estoque — ATENDENTE registra entradas/saídas manuais; ADMIN tem acesso total
    @PostMapping("/{id}/movimentacoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<MovimentacaoResponseDTO> registrarMovimentacao(
            @PathVariable UUID id,
            @Valid @RequestBody MovimentacaoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pecaService.registrarMovimentacao(id, dto));
    }
}