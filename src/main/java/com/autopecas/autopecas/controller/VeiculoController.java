package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.dto.veiculo.VeiculoCreateDTO;
import com.autopecas.autopecas.dto.veiculo.VeiculoResponseDTO;
import com.autopecas.autopecas.dto.veiculo.VeiculoUpdateDTO;
import com.autopecas.autopecas.service.VeiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/veiculos")
@RequiredArgsConstructor
public class VeiculoController {

    private final VeiculoService veiculoService;

    @GetMapping
    public ResponseEntity<List<VeiculoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(veiculoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeiculoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(veiculoService.buscarPorId(id));
    }

    @GetMapping("/placa/{placa}")
    public ResponseEntity<VeiculoResponseDTO> buscarPorPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(veiculoService.buscarPorPlaca(placa));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<VeiculoResponseDTO>> listarPorCliente(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(veiculoService.listarPorCliente(clienteId));
    }

    @PostMapping
    public ResponseEntity<VeiculoResponseDTO> criar(@Valid @RequestBody VeiculoCreateDTO dto) {
        VeiculoResponseDTO criado = veiculoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VeiculoResponseDTO> atualizar(@PathVariable UUID id, @RequestBody VeiculoUpdateDTO dto) {
        return ResponseEntity.ok(veiculoService.atualizar(id, dto));

    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        veiculoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}


