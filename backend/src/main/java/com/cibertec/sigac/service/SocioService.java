package com.cibertec.sigac.service;

import java.util.List;

import com.cibertec.sigac.dto.SocioRequest;
import com.cibertec.sigac.dto.SocioResponse;

public interface SocioService {

    List<SocioResponse> listarTodos();

    SocioResponse obtenerPorId(Long id);

    SocioResponse crear(SocioRequest request);

    SocioResponse actualizar(Long id, SocioRequest request);

    void eliminar(Long id);
}
