package com.cibertec.mscatalogos.negocio;

import java.util.List;

import com.cibertec.mscatalogos.dto.GiroRequest;
import com.cibertec.mscatalogos.dto.GiroResponse;

public interface GiroService {

    List<GiroResponse> listarTodos();

    GiroResponse obtenerPorId(Long id);

    GiroResponse crear(GiroRequest request);

    GiroResponse actualizar(Long id, GiroRequest request);

    void eliminar(Long id);
}
