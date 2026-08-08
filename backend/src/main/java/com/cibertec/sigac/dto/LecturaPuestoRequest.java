package com.cibertec.sigac.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LecturaPuestoRequest {

    @NotNull(message = "El puesto es obligatorio")
    private Long puestoId;

    @NotNull(message = "La lectura inicial es obligatoria")
    @PositiveOrZero(message = "La lectura inicial no puede ser negativa")
    private BigDecimal lecturaInicial;

    @NotNull(message = "La lectura final es obligatoria")
    @PositiveOrZero(message = "La lectura final no puede ser negativa")
    private BigDecimal lecturaFinal;
}
