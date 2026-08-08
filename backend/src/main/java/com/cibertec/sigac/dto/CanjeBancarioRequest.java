package com.cibertec.sigac.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CanjeBancarioRequest {

    @NotNull(message = "La cuenta es obligatoria")
    private Long cuentaId;

    @NotNull(message = "El banco es obligatorio")
    private Long bancoId;

    @NotNull(message = "La fecha de deposito es obligatoria")
    private LocalDate fechaDeposito;
}
