package com.cibertec.mscobranza.negocio;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.cibertec.mscobranza.client.CatalogosClient;
import com.cibertec.mscobranza.dto.BancoResponse;
import com.cibertec.mscobranza.dto.PuestoResponse;
import com.cibertec.mscobranza.dto.ServicioCobrableResponse;
import com.cibertec.mscobranza.dto.SocioResponse;
import com.cibertec.sigac.common.exception.ResourceNotFoundException;

public class CatalogoResolver {

    private final CatalogosClient client;
    private final Map<Long, Optional<SocioResponse>> socios = new HashMap<>();
    private final Map<Long, Optional<PuestoResponse>> puestos = new HashMap<>();
    private final Map<Long, Optional<ServicioCobrableResponse>> servicios = new HashMap<>();
    private final Map<Long, Optional<BancoResponse>> bancos = new HashMap<>();

    public CatalogoResolver(CatalogosClient client) {
        this.client = client;
    }

    public SocioResponse socio(Long id) {
        return resolver(socios, id, client::obtenerSocio);
    }

    public PuestoResponse puesto(Long id) {
        return resolver(puestos, id, client::obtenerPuesto);
    }

    public ServicioCobrableResponse servicio(Long id) {
        return resolver(servicios, id, client::obtenerServicio);
    }

    public BancoResponse banco(Long id) {
        return resolver(bancos, id, client::obtenerBanco);
    }

    private <T> T resolver(Map<Long, Optional<T>> cache, Long id, Function<Long, T> consulta) {
        if (id == null) {
            return null;
        }
        Optional<T> guardado = cache.get(id);
        if (guardado == null) {
            try {
                guardado = Optional.ofNullable(consulta.apply(id));
            } catch (ResourceNotFoundException ex) {
                guardado = Optional.empty();
            }
            cache.put(id, guardado);
        }
        return guardado.orElse(null);
    }
}
