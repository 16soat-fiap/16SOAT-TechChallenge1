package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.dto.veiculo.VeiculoCreateDTO;
import com.autopecas.autopecas.dto.veiculo.VeiculoResponseDTO;
import com.autopecas.autopecas.dto.veiculo.VeiculoUpdateDTO;
import com.autopecas.autopecas.service.VeiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/veiculos")
@RequiredArgsConstructor
public class VeiculoController {

    private final VeiculoService veiculoService;

    // CLIENTE não pode listar todos os veículos — apenas os seus (por clienteId)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<List<VeiculoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(veiculoService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'CLIENTE')")
    public ResponseEntity<VeiculoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(veiculoService.buscarPorId(id));
    }

    @GetMapping("/placa/{placa}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<VeiculoResponseDTO> buscarPorPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(veiculoService.buscarPorPlaca(placa));
    }

    // CLIENTE pode listar os próprios veículos filtrando pelo seu clienteId
    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'CLIENTE')")
    public ResponseEntity<List<VeiculoResponseDTO>> listarPorCliente(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(veiculoService.listarPorCliente(clienteId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<VeiculoResponseDTO> criar(@Valid @RequestBody VeiculoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(veiculoService.criar(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<VeiculoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody VeiculoUpdateDTO dto) {
        return ResponseEntity.ok(veiculoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        veiculoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}