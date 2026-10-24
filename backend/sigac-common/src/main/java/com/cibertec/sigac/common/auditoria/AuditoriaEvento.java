package com.cibertec.sigac.common.auditoria;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mensaje que los servicios operativos (cobranza, egresos) publican en
 * RabbitMQ y que ms-auditoria consume para guardar la bitacora: responde a
 * "quien hizo que movimiento y cuando" (RNF-14).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaEvento {

    private String modulo;
    private String accion;
    private String referencia;
    private String detalle;
    private BigDecimal monto;
    private String usuario;
    private LocalDateTime fecha;
}
