package com.cibertec.sigac.service;

import java.util.List;

import com.cibertec.sigac.dto.ServicioCobrableRequest;
import com.cibertec.sigac.dto.ServicioCobrableResponse;

public interface ServicioCobrableService {

    List<ServicioCobrableResponse> listarTodos();

    ServicioCobrableResponse obtenerPorId(Long id);

    ServicioCobrableResponse crear(ServicioCobrableRequest request);

    ServicioCobrableResponse actualizar(Long id, ServicioCobrableRequest request);

    void eliminar(Long id);
}
