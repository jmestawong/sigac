package com.cibertec.mscatalogos.dto;

import java.math.BigDecimal;

import com.cibertec.mscatalogos.entidades.Moneda;
import com.cibertec.mscatalogos.entidades.Recurrencia;
import com.cibertec.mscatalogos.entidades.TipoDestinatario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicioCobrableResponse {

    private Long id;
    private String nombre;
    private Recurrencia recurrencia;
    private BigDecimal costo;
    private Moneda moneda;
    private TipoDestinatario destinatario;
    private boolean esPorConsumo;
}
