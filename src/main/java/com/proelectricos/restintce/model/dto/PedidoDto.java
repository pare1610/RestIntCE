package com.proelectricos.restintce.model.dto;

import lombok.Value;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for {@link com.proelectricos.restintce.model.entity.Pedido}
 */
@Value
public class PedidoDto implements Serializable {
    Integer id;
    String num;
    String cliente;
    Instant fecha;
    String cod;
    String nom;
    String ud;
    BigDecimal cant;
    BigDecimal costo;
}