package com.cibertec.sigac.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cibertec.sigac.entity.EstadoEgreso;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EgresoResponse {

    private Long id;
    private String correlativo;
    private String numeroDocumento;
    private String proveedor;
    private LocalDate fecha;
    private BigDecimal importe;
    private String documentoAsociado;
    private String motivo;
    private EstadoEgreso estado;
}
