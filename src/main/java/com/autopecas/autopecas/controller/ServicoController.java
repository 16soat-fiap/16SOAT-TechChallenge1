package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.dto.servico.ServicoCreateDTO;
import com.autopecas.autopecas.dto.servico.ServicoResponseDTO;
import com.autopecas.autopecas.service.ServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servicos")
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService servicoService;

    // MECANICO consulta serviços disponíveis para orçar/executar
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<List<ServicoResponseDTO>> listar() {
        return ResponseEntity.ok(servicoService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<ServicoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(servicoService.buscarPorId(id));
    }

    // Cadastro, edição e desativação — somente ADMIN
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServicoResponseDTO> criar(@Valid @RequestBody ServicoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicoService.criar(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServicoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ServicoCreateDTO dto) {
        return ResponseEntity.ok(servicoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        servicoService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}