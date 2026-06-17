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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pecas")
@RequiredArgsConstructor
public class PecaController {

    private final PecaService pecaService;

    @GetMapping
    public ResponseEntity<List<PecaResponseDTO>> listar(@RequestParam(required = false) Boolean estoqueBaixo) {
        return ResponseEntity.ok(pecaService.listar(estoqueBaixo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PecaResponseDTO> buscarPorId(@PathVariable UUID id) {
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
        return ResponseEntity.status(HttpStatus.CREATED).body(pecaService.registrarMovimentacao(id, dto));
    }
}
