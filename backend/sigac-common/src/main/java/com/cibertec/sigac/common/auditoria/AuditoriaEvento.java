package com.cibertec.sigac.common.auditoria;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
