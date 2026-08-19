package com.autopecas.autopecas.adapter.in.web;

import com.autopecas.autopecas.adapter.in.web.dto.veiculo.VeiculoCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.veiculo.VeiculoResponseDTO;
import com.autopecas.autopecas.adapter.in.web.dto.veiculo.VeiculoUpdateDTO;
import com.autopecas.autopecas.adapter.in.web.mapper.VeiculoWebMapper;
import com.autopecas.autopecas.application.port.in.GestaoDeVeiculos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Adapter de entrada HTTP do agregado Veiculo. */
@RestController
@RequestMapping("/api/veiculos")
public class VeiculoController {

    private final GestaoDeVeiculos gestaoDeVeiculos;
    private final VeiculoWebMapper mapper;

    public VeiculoController(GestaoDeVeiculos gestaoDeVeiculos, VeiculoWebMapper mapper) {
        this.gestaoDeVeiculos = gestaoDeVeiculos;
        this.mapper = mapper;
    }

    // CLIENTE não pode listar todos os veículos — apenas os seus (por clienteId)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<List<VeiculoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(gestaoDeVeiculos.listarAtivos().stream()
                .map(mapper::paraResposta).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'CLIENTE')")
    public ResponseEntity<VeiculoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.paraResposta(gestaoDeVeiculos.porId(id)));
    }

    @GetMapping("/placa/{placa}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<VeiculoResponseDTO> buscarPorPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(mapper.paraResposta(gestaoDeVeiculos.porPlaca(placa)));
    }

    // CLIENTE pode listar os próprios veículos filtrando pelo seu clienteId
    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'CLIENTE')")
    public ResponseEntity<List<VeiculoResponseDTO>> listarPorCliente(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(gestaoDeVeiculos.doCliente(clienteId).stream()
                .map(mapper::paraResposta).toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<VeiculoResponseDTO> criar(@Valid @RequestBody VeiculoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.paraResposta(gestaoDeVeiculos.cadastrar(mapper.paraComando(dto))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<VeiculoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody VeiculoUpdateDTO dto) {
        return ResponseEntity.ok(
                mapper.paraResposta(gestaoDeVeiculos.atualizar(id, mapper.paraComando(dto))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        gestaoDeVeiculos.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
