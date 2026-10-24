package com.cibertec.mscatalogos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cuantos registros de cobranza (cuentas o recibos) usan un elemento del catalogo. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenciaResponse {

    private String tipo;
    private Long id;
    private long cantidad;
}
