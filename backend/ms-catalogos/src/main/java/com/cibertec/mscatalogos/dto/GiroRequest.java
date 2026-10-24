package com.cibertec.mscatalogos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GiroRequest {

    @NotBlank(message = "El nombre del giro es obligatorio")
    private String nombre;
}
