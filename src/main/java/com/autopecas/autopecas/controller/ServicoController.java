package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.dto.servico.ServicoCreateDTO;
import com.autopecas.autopecas.dto.servico.ServicoResponseDTO;
import com.autopecas.autopecas.service.ServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servicos")
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService servicoService;

    @GetMapping
    public ResponseEntity<List<ServicoResponseDTO>> listar(){
        return ResponseEntity.ok(servicoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(servicoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ServicoResponseDTO> criar(@Valid @RequestBody ServicoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicoService.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ServicoCreateDTO dto) {
        return ResponseEntity.ok(servicoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        servicoService.desativar(id);
        return ResponseEntity.noContent().build();
    }

}
