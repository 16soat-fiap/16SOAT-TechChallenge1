package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.dto.os.*;
import com.autopecas.autopecas.service.OrdemServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ordens-servico")
@RequiredArgsConstructor
public class OrdemServicoController {

    private final OrdemServicoService ordemServicoService;

    @GetMapping
    public ResponseEntity<Page<OrdemServicoResponseDTO>> listar(
            @RequestParam(required = false) StatusOS status,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) UUID mecanicoId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ordemServicoService.listar(status, clienteId, mecanicoId, pageable));
    }

    @GetMapping("/{numero}")
    public ResponseEntity<OrdemServicoResponseDTO> buscarPorNumero(@PathVariable String numero) {
        return ResponseEntity.ok(ordemServicoService.buscarPorNumero(numero));
    }

    @PostMapping
    public ResponseEntity<OrdemServicoResponseDTO> criar(
            @Valid @RequestBody OrdemServicoCreateDTO dto,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(ordemServicoService.criar(dto, email));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrdemServicoResponseDTO> avancarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AvancarStatusDTO dto,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ordemServicoService.avancarStatus(id, dto, email));
    }

    @PatchMapping("/{id}/diagnostico")
    public ResponseEntity<OrdemServicoResponseDTO> registrarDiagnostico(
            @PathVariable UUID id,
            @Valid @RequestBody DiagnosticoDTO dto) {
        return ResponseEntity.ok(ordemServicoService.registrarDiagnostico(id, dto));
    }

    @PatchMapping("/{id}/mecanico")
    public ResponseEntity<OrdemServicoResponseDTO> atribuirMecanico(
            @PathVariable UUID id,
            @Valid @RequestBody AtribuirMecanicoDTO dto) {
        return ResponseEntity.ok(ordemServicoService.atribuirMecanico(id, dto));
    }
}
