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
public class SocioResponse {

    private Long id;
    private String codigo;
    private String nombres;
    private String apellidos;
    private String accion;
    private String etapa;
    private LocalDate fechaNacimiento;
}
