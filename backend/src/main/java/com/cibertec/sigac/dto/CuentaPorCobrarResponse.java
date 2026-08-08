package com.cibertec.sigac.dto;

import java.math.BigDecimal;

import com.cibertec.sigac.entity.EstadoCuenta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaPorCobrarResponse {

    private Long id;
    private EstadoCuenta estado;
    private BigDecimal monto;
    private String periodo;
    private PuestoResponse puesto;
    private SocioResponse socio;
    private ServicioCobrableResponse servicio;
    private BigDecimal lecturaInicial;
    private BigDecimal lecturaFinal;
}
