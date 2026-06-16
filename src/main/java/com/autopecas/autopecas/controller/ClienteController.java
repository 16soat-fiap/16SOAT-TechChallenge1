package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.dto.cliente.ClienteCreatePFDTO;
import com.autopecas.autopecas.dto.cliente.ClienteCreatePJDTO;
import com.autopecas.autopecas.dto.cliente.ClienteResponseDTO;
import com.autopecas.autopecas.dto.cliente.ClienteUpdateDTO;
import com.autopecas.autopecas.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> listar(){
        return ResponseEntity.ok(clienteService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable UUID id){
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @GetMapping("/buscarDOC")
    public ResponseEntity<ClienteResponseDTO> buscarPorDocumento(@RequestParam String documento){
        return  ResponseEntity.ok(clienteService.buscarPorDocumento(documento));
    }

    @PostMapping("/pf")
    public ResponseEntity<ClienteResponseDTO> criarPF(@Valid @RequestBody ClienteCreatePFDTO dto){
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.criarPF(dto));
    }

    @PostMapping("/pj")
    public ResponseEntity<ClienteResponseDTO> criarPJ(@Valid @RequestBody ClienteCreatePJDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.criarPJ(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ClienteUpdateDTO dto) {
        return ResponseEntity.ok(clienteService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        clienteService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
