package com.cibertec.sigac.entity;

import java.math.BigDecimal;

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

@Entity
@Table(name = "cuentas_por_cobrar")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaPorCobrar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoCuenta estado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 20)
    private String periodo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "puesto_id", nullable = true)
    private Puesto puesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "socio_id", nullable = true)
    private Socio socio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private ServicioCobrable servicio;

    @Column(name = "lectura_inicial", precision = 12, scale = 2)
    private BigDecimal lecturaInicial;

    @Column(name = "lectura_final", precision = 12, scale = 2)
    private BigDecimal lecturaFinal;
}
