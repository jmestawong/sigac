package com.cibertec.sigac.dto;

import com.cibertec.sigac.entity.Moneda;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BancoRequest {

    @NotBlank(message = "El nombre del banco es obligatorio")
    private String nombre;

    @NotBlank(message = "El numero de cuenta es obligatorio")
    private String numeroCuenta;

    @NotBlank(message = "El CCI es obligatorio")
    private String cci;

    @NotNull(message = "La moneda es obligatoria")
    private Moneda moneda;
}
