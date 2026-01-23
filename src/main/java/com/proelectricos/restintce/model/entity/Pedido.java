package com.proelectricos.restintce.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@ToString
@Entity
@Table(name = "PEDIDOS")
public class Pedido {

    @Column(name = "NUM", length = 7)
    private String num;

    @Column(name = "CLIENTE", length = 15)
    private String cliente;

    @Column(name = "FECHA")
    private Instant fecha;

    @Column(name = "COD", length = 20)
    private String cod;

    @Column(name = "NOM", length = 200)
    private String nom;

    @Column(name = "UD", length = 2)
    private String ud;

    @Column(name = "CANT", precision = 14, scale = 4)
    private BigDecimal cant;

    @Column(name = "COSTO", precision = 16, scale = 4)
    private BigDecimal costo;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;


}