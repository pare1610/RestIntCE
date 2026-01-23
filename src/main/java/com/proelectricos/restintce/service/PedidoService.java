package com.proelectricos.restintce.service;

import com.proelectricos.restintce.model.dto.PedidoDto;
import com.proelectricos.restintce.model.entity.Pedido;
import com.proelectricos.restintce.model.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository repository;

    // --- LECTURA ---

    @Transactional(readOnly = true)
    public List<PedidoDto> getAll() {
        // Filtrar por fecha mayor o igual a 2025-01-01
        Instant fechaInicio = Instant.parse("2025-01-01T00:00:00Z");
        List<Pedido> pedidos = repository.findByFechaGreaterThanEqual(fechaInicio);
        
        log.info("Resultados de la consulta (Desde 2025-01-01, Total: {}):", pedidos.size());
        pedidos.forEach(p -> log.info("Pedido: {}", p));
        
        return pedidos.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PedidoDto getById(Integer id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<PedidoDto> getByNum(String num) {
        List<Pedido> pedidos = repository.findByNum(num);
        if (pedidos.isEmpty()) {
            throw new RuntimeException("Pedido no encontrado con NUM: " + num);
        }
        return pedidos.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // --- ESCRITURA ---

    @Transactional
    public PedidoDto create(PedidoDto dto) {
        // Nota: Asumimos que el ID se genera automáticamente en BD o es nulo al crear
        Pedido entity = new Pedido();
        if (dto.getId() != null) {
            entity.setId(dto.getId());
        }
        mapDtoToEntity(dto, entity);

        Pedido saved = repository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public PedidoDto update(Integer id, PedidoDto dto) {
        Pedido entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se puede actualizar, ID no existe: " + id));

        mapDtoToEntity(dto, entity); // Actualizamos campos
        Pedido updated = repository.save(entity);
        return toDto(updated);
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar, ID no existe: " + id);
        }
        repository.deleteById(id);
    }

    // --- MAPPERS (Privados para encapsular lógica de conversión) ---

    private PedidoDto toDto(Pedido entity) {
        // Tu DTO es inmutable (@Value), usamos el constructor
        return new PedidoDto(
                entity.getId(),
                entity.getNum(),
                entity.getCliente(),
                entity.getFecha(),
                entity.getCod(),
                entity.getNom(),
                entity.getUd(),
                entity.getCant(),
                entity.getCosto()
        );
    }

    private void mapDtoToEntity(PedidoDto dto, Pedido entity) {
        entity.setNum(dto.getNum());
        entity.setCliente(dto.getCliente());
        entity.setFecha(dto.getFecha());
        entity.setCod(dto.getCod());
        entity.setNom(dto.getNom());
        entity.setUd(dto.getUd());
        entity.setCant(dto.getCant());
        entity.setCosto(dto.getCosto());
        // No mapeamos ID aquí porque viene de la URL o es autogenerado
    }
}