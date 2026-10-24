package com.cibertec.mscatalogos.negocio;

import java.util.List;

import com.cibertec.mscatalogos.dto.PuestoRequest;
import com.cibertec.mscatalogos.dto.PuestoResponse;

public interface PuestoService {

    List<PuestoResponse> listarTodos();

    PuestoResponse obtenerPorId(Long id);

    PuestoResponse crear(PuestoRequest request);

    PuestoResponse actualizar(Long id, PuestoRequest request);

    void eliminar(Long id);
}
