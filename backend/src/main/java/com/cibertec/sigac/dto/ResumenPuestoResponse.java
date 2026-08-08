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
public class ResumenPuestoResponse {

    private PuestoResponse puesto;
    private List<CuentaPorCobrarResponse> cuentasPuesto;
    private List<CuentaPorCobrarResponse> cuentasSocioAsociado;
    private List<ReciboResponse> movimientos;
}
