package com.proelectricos.restintce.model.repository;

import com.proelectricos.restintce.model.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    // Spring deriva la consulta automáticamente: SELECT * FROM PEDIDOS WHERE NUM = ?
    List<Pedido> findByNum(String num);

    List<Pedido> findByFechaGreaterThanEqual(Instant fecha);
}