package com.cibertec.mscatalogos.negocio;

import java.util.List;

import com.cibertec.mscatalogos.dto.ServicioCobrableRequest;
import com.cibertec.mscatalogos.dto.ServicioCobrableResponse;

public interface ServicioCobrableService {

    List<ServicioCobrableResponse> listarTodos();

    ServicioCobrableResponse obtenerPorId(Long id);

    ServicioCobrableResponse crear(ServicioCobrableRequest request);

    ServicioCobrableResponse actualizar(Long id, ServicioCobrableRequest request);

    void eliminar(Long id);
}
