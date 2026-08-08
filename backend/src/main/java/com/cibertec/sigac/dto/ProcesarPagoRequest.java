package com.cibertec.sigac.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcesarPagoRequest {

    @NotNull
    private List<Long> cuentasAbonadasIds;

    @NotNull
    private List<Long> cuentasExoneradasIds;
}
