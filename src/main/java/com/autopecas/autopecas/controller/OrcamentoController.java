package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.dto.orcamento.AprovarRejeitarDTO;
import com.autopecas.autopecas.dto.orcamento.OrcamentoCreateDTO;
import com.autopecas.autopecas.dto.orcamento.OrcamentoResponseDTO;
import com.autopecas.autopecas.service.OrcamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ordens-servico/{osId}/orcamentos")
@RequiredArgsConstructor
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    // Criar orçamento é responsabilidade do ATENDENTE
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<OrcamentoResponseDTO> criar(
            @PathVariable UUID osId,
            @Valid @RequestBody OrcamentoCreateDTO orcamentoDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orcamentoService.criarOrcamento(osId, orcamentoDto));
    }

    // CLIENTE pode listar os orçamentos da própria OS
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'CLIENTE')")
    public ResponseEntity<List<OrcamentoResponseDTO>> listar(@PathVariable UUID osId) {
        return ResponseEntity.ok(orcamentoService.listar(osId));
    }

    // Enviar orçamento ao cliente — responsabilidade do ATENDENTE
    @PatchMapping("/{id}/enviar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<OrcamentoResponseDTO> enviar(
            @PathVariable UUID osId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(orcamentoService.enviar(osId, id));
    }

    // Aprovar e rejeitar são ações do CLIENTE (ou ADMIN/ATENDENTE em nome dele)
    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'CLIENTE')")
    public ResponseEntity<OrcamentoResponseDTO> aprovar(
            @PathVariable UUID osId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(orcamentoService.aprovar(osId, id));
    }

    @PatchMapping("/{id}/rejeitar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'CLIENTE')")
    public ResponseEntity<OrcamentoResponseDTO> rejeitar(
            @PathVariable UUID osId,
            @PathVariable UUID id,
            @RequestBody(required = false) AprovarRejeitarDTO dto) {
        AprovarRejeitarDTO motivo = dto != null ? dto : new AprovarRejeitarDTO(null);
        return ResponseEntity.ok(orcamentoService.rejeitar(osId, id, motivo));
    }
}