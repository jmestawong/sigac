package com.cibertec.sigac.service;

import java.util.List;

import com.cibertec.sigac.dto.PuestoRequest;
import com.cibertec.sigac.dto.PuestoResponse;

public interface PuestoService {

    List<PuestoResponse> listarTodos();

    PuestoResponse obtenerPorId(Long id);

    PuestoResponse crear(PuestoRequest request);

    PuestoResponse actualizar(Long id, PuestoRequest request);

    void eliminar(Long id);
}
