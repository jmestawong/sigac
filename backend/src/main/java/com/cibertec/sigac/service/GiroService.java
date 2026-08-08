package com.cibertec.sigac.service;

import java.util.List;

import com.cibertec.sigac.dto.GiroRequest;
import com.cibertec.sigac.dto.GiroResponse;

public interface GiroService {

    List<GiroResponse> listarTodos();

    GiroResponse obtenerPorId(Long id);

    GiroResponse crear(GiroRequest request);

    GiroResponse actualizar(Long id, GiroRequest request);

    void eliminar(Long id);
}
