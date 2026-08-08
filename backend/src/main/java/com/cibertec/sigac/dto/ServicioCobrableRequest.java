package com.cibertec.sigac.dto;

import java.math.BigDecimal;

import com.cibertec.sigac.entity.Moneda;
import com.cibertec.sigac.entity.Recurrencia;
import com.cibertec.sigac.entity.TipoDestinatario;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicioCobrableRequest {

    @NotBlank(message = "El nombre del servicio es obligatorio")
    private String nombre;

    @NotNull(message = "La recurrencia es obligatoria")
    private Recurrencia recurrencia;

    @NotNull(message = "El costo es obligatorio")
    @DecimalMin(value = "0.01", message = "El costo debe ser mayor a 0")
    private BigDecimal costo;

    @NotNull(message = "La moneda es obligatoria")
    private Moneda moneda;

    @NotNull(message = "El destinatario es obligatorio")
    private TipoDestinatario destinatario;

    private boolean esPorConsumo;
}
