package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.dto.orcamento.AprovarRejeitarDTO;
import com.autopecas.autopecas.dto.orcamento.OrcamentoCreateDTO;
import com.autopecas.autopecas.dto.orcamento.OrcamentoResponseDTO;
import com.autopecas.autopecas.service.OrcamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ordens-servico/{osId}/orcamentos")
@RequiredArgsConstructor
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    @PostMapping
    public ResponseEntity<OrcamentoResponseDTO> criar(
            @PathVariable UUID osId,
            @Valid @RequestBody OrcamentoCreateDTO Orcamentodto){
        return  ResponseEntity.status(HttpStatus.CREATED).body(orcamentoService.criarOrcamento(osId, Orcamentodto));
    }

    @GetMapping
    public ResponseEntity<List<OrcamentoResponseDTO>> listar(@PathVariable UUID osId) {
        return ResponseEntity.ok(orcamentoService.listar(osId));
    }

    @PatchMapping("/{id}/enviar")
    public ResponseEntity<OrcamentoResponseDTO> enviar(
            @PathVariable UUID osId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(orcamentoService.enviar(osId, id));
    }

    @PatchMapping("/{id}/aprovar")
    public ResponseEntity<OrcamentoResponseDTO> aprovar(
            @PathVariable UUID osId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(orcamentoService.aprovar(osId, id));
    }

    @PatchMapping("/{id}/rejeitar")
    public ResponseEntity<OrcamentoResponseDTO> rejeitar(
            @PathVariable UUID osId,
            @PathVariable UUID id,
            @RequestBody(required = false)AprovarRejeitarDTO dto){
        AprovarRejeitarDTO motivo = dto != null ? dto : new AprovarRejeitarDTO(null);
        return ResponseEntity.ok(orcamentoService.rejeitar(osId, id, motivo));
    }

}
