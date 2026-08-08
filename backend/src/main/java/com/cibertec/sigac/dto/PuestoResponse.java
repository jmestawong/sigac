package com.cibertec.sigac.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuestoResponse {

    private Long id;
    private String numero;
    private String nombreInquilino;
    private LocalDate fechaInicioVigencia;
    private LocalDate fechaFinVigencia;
    private GiroResponse giro;
    private SocioResponse socio;
}
