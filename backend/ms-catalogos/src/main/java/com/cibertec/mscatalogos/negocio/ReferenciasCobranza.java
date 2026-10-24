package com.cibertec.mscatalogos.negocio;

import org.springframework.stereotype.Component;

import com.cibertec.mscatalogos.client.CobranzaClient;
import com.cibertec.mscatalogos.dto.ReferenciaResponse;
import com.cibertec.sigac.common.exception.DuplicateResourceException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReferenciasCobranza {

    private final CobranzaClient cobranzaClient;

    public void validarSinUso(String tipo, Long id, String descripcion) {
        ReferenciaResponse referencia = cobranzaClient.referencias(tipo, id);
        if (referencia != null && referencia.getCantidad() > 0) {
            throw new DuplicateResourceException("No se puede eliminar " + descripcion + " porque tiene "
                    + referencia.getCantidad() + " registro(s) de cobranza asociados");
        }
    }
}
