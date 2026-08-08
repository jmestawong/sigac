package com.cibertec.sigac.dto;

import com.cibertec.sigac.entity.Moneda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BancoResponse {

    private Long id;
    private String nombre;
    private String numeroCuenta;
    private String cci;
    private Moneda moneda;
}
