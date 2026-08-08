package com.cibertec.sigac.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * El correlativo se asigna despues de persistir (dos fases dentro de la
 * misma transaccion), por eso la columna admite null a nivel de esquema
 * aunque en la practica ningun recibo confirmado queda sin el.
 */
@Entity
@Table(name = "recibos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recibo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String correlativo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRecibo tipo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "banco_id", nullable = true)
    private Banco banco;

    @Column(name = "fecha_deposito")
    private LocalDate fechaDeposito;

    @Column(length = 150)
    private String depositante;

    @Column(length = 100)
    private String categoria;

    @Column(length = 255)
    private String concepto;
}
