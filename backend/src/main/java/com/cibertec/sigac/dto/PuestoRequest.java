package com.cibertec.sigac.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PuestoRequest {

    @NotBlank(message = "El numero de puesto es obligatorio")
    private String numero;

    @NotBlank(message = "El nombre del inquilino es obligatorio")
    private String nombreInquilino;

    @NotNull(message = "La fecha de inicio de vigencia es obligatoria")
    private LocalDate fechaInicioVigencia;

    @NotNull(message = "La fecha de fin de vigencia es obligatoria")
    private LocalDate fechaFinVigencia;

    @NotNull(message = "El giro es obligatorio")
    private Long giroId;

    private Long socioId;
}
