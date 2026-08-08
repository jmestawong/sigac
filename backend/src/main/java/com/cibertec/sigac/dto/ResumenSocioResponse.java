package com.cibertec.sigac.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenSocioResponse {

    private SocioResponse socio;
    private List<CuentaPorCobrarResponse> cuentasSocio;
    private List<CuentaPorCobrarResponse> cuentasPuestos;
    private List<ReciboResponse> movimientos;
}
