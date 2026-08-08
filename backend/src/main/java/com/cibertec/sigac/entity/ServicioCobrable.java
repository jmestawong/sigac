package com.cibertec.sigac.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "servicios_cobrables")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicioCobrable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Recurrencia recurrencia;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Moneda moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoDestinatario destinatario;

    @Column(name = "es_por_consumo", nullable = false)
    private boolean esPorConsumo;
}
