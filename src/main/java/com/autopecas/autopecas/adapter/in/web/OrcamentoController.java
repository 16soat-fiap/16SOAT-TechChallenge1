package com.autopecas.autopecas.adapter.in.web;

import com.autopecas.autopecas.adapter.in.web.dto.orcamento.AprovarRejeitarDTO;
import com.autopecas.autopecas.adapter.in.web.dto.orcamento.OrcamentoCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.orcamento.OrcamentoResponseDTO;
import com.autopecas.autopecas.adapter.in.web.mapper.OrcamentoWebMapper;
import com.autopecas.autopecas.application.port.in.GestaoDeOrcamentos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Adapter de entrada HTTP do agregado Orcamento, aninhado sob a OS. */
@RestController
@RequestMapping("/api/ordens-servico/{osId}/orcamentos")
public class OrcamentoController {

    private final GestaoDeOrcamentos gestaoDeOrcamentos;
    private final OrcamentoWebMapper mapper;

    public OrcamentoController(GestaoDeOrcamentos gestaoDeOrcamentos, OrcamentoWebMapper mapper) {
        this.gestaoDeOrcamentos = gestaoDeOrcamentos;
        this.mapper = mapper;
    }

    // Criar orçamento é responsabilidade do ATENDENTE
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<OrcamentoResponseDTO> criar(
            @PathVariable UUID osId,
            @Valid @RequestBody OrcamentoCreateDTO orcamentoDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.paraResposta(
                gestaoDeOrcamentos.criar(osId, mapper.paraComando(orcamentoDto))));
    }

    // CLIENTE pode listar os orçamentos da própria OS
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'CLIENTE')")
    public ResponseEntity<List<OrcamentoResponseDTO>> listar(@PathVariable UUID osId) {
        return ResponseEntity.ok(gestaoDeOrcamentos.daOrdemServico(osId).stream()
                .map(mapper::paraResposta).toList());
    }

    // Enviar orçamento ao cliente — responsabilidade do ATENDENTE
    @PatchMapping("/{id}/enviar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<OrcamentoResponseDTO> enviar(
            @PathVariable UUID osId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(mapper.paraResposta(gestaoDeOrcamentos.enviar(osId, id)));
    }

    // Aprovar e rejeitar são ações do CLIENTE (ou ADMIN/ATENDENTE em nome dele)
    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'CLIENTE')")
    public ResponseEntity<OrcamentoResponseDTO> aprovar(
            @PathVariable UUID osId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(mapper.paraResposta(gestaoDeOrcamentos.aprovar(osId, id)));
    }

    @PatchMapping("/{id}/rejeitar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'CLIENTE')")
    public ResponseEntity<OrcamentoResponseDTO> rejeitar(
            @PathVariable UUID osId,
            @PathVariable UUID id,
            @RequestBody(required = false) AprovarRejeitarDTO dto) {
        String motivo = dto != null ? dto.motivo() : null;
        return ResponseEntity.ok(mapper.paraResposta(gestaoDeOrcamentos.rejeitar(osId, id, motivo)));
    }
}
