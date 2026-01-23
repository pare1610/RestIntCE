package com.proelectricos.restintce.controller;

import com.proelectricos.restintce.model.dto.PedidoDto;
import com.proelectricos.restintce.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/integrador")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService service;

    @GetMapping
    public ResponseEntity<List<PedidoDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // Endpoint específico para buscar por NUM
    @GetMapping("/buscar/{num}")
    public ResponseEntity<List<PedidoDto>> getByNum(@PathVariable String num) {
        return ResponseEntity.ok(service.getByNum(num));
    }

    @PostMapping
    public ResponseEntity<PedidoDto> create(@RequestBody PedidoDto dto) {
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PedidoDto> update(@PathVariable Integer id, @RequestBody PedidoDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}