package com.cibertec.mscatalogos.negocio;

import java.util.List;

import com.cibertec.mscatalogos.dto.SocioRequest;
import com.cibertec.mscatalogos.dto.SocioResponse;

public interface SocioService {

    List<SocioResponse> listarTodos();

    SocioResponse obtenerPorId(Long id);

    SocioResponse crear(SocioRequest request);

    SocioResponse actualizar(Long id, SocioRequest request);

    void eliminar(Long id);
}
