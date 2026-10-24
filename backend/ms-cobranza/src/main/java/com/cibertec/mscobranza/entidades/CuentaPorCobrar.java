package com.cibertec.mscobranza.entidades;

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

/**
 * Los socios, puestos y servicios pertenecen a ms-catalogos (otra base de
 * datos), por eso aqui solo se guardan sus identificadores; los datos se
 * consultan con OpenFeign cuando hay que mostrarlos.
 */
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

    @Column(name = "puesto_id")
    private Long puestoId;

    @Column(name = "socio_id")
    private Long socioId;

    @Column(name = "servicio_id", nullable = false)
    private Long servicioId;

    @Column(name = "lectura_inicial", precision = 12, scale = 2)
    private BigDecimal lecturaInicial;

    @Column(name = "lectura_final", precision = 12, scale = 2)
    private BigDecimal lecturaFinal;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "recibo_id", nullable = true)
    private Recibo recibo;
}
