package com.cibertec.sigac.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.cibertec.sigac.entity.TipoRecibo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReciboResponse {

    private Long id;
    private String correlativo;
    private TipoRecibo tipo;
    private LocalDateTime fecha;
    private BigDecimal monto;
    private BancoResponse banco;
    private LocalDate fechaDeposito;
    private String depositante;
    private String categoria;
    private String concepto;
    private List<CuentaPorCobrarResponse> cuentas;
}
