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
public class ProcesarPagoResponse {

    private ReciboResponse recibo;
    private List<CuentaPorCobrarResponse> cuentasActualizadas;
}
