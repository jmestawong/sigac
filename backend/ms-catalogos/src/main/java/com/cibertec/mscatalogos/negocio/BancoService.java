package com.cibertec.mscatalogos.negocio;

import java.util.List;

import com.cibertec.mscatalogos.dto.BancoRequest;
import com.cibertec.mscatalogos.dto.BancoResponse;

public interface BancoService {

    List<BancoResponse> listarTodos();

    BancoResponse obtenerPorId(Long id);

    BancoResponse crear(BancoRequest request);

    BancoResponse actualizar(Long id, BancoRequest request);

    void eliminar(Long id);
}
