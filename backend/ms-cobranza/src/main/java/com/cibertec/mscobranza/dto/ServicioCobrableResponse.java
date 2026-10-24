package com.cibertec.mscobranza.dto;

import java.math.BigDecimal;

import com.cibertec.mscobranza.dto.Moneda;
import com.cibertec.mscobranza.dto.Recurrencia;
import com.cibertec.mscobranza.dto.TipoDestinatario;

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
