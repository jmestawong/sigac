package com.cibertec.sigac.service;

import java.util.List;

import com.cibertec.sigac.dto.BancoRequest;
import com.cibertec.sigac.dto.BancoResponse;

public interface BancoService {

    List<BancoResponse> listarTodos();

    BancoResponse obtenerPorId(Long id);

    BancoResponse crear(BancoRequest request);

    BancoResponse actualizar(Long id, BancoRequest request);

    void eliminar(Long id);
}
