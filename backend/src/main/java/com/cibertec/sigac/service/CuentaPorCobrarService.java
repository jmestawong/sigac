package com.cibertec.sigac.service;

import java.util.List;

import com.cibertec.sigac.dto.CuentaPorCobrarResponse;
import com.cibertec.sigac.dto.GenerarPuestosConsumoRequest;
import com.cibertec.sigac.dto.GenerarPuestosMontoFijoRequest;
import com.cibertec.sigac.dto.GenerarSociosRequest;

public interface CuentaPorCobrarService {

    List<CuentaPorCobrarResponse> listarTodas();

    CuentaPorCobrarResponse obtenerPorId(Long id);

    void eliminar(Long id);

    List<CuentaPorCobrarResponse> generarParaPuestosMontoFijo(GenerarPuestosMontoFijoRequest request);

    List<CuentaPorCobrarResponse> generarParaPuestosConsumo(GenerarPuestosConsumoRequest request);

    List<CuentaPorCobrarResponse> generarParaSocios(GenerarSociosRequest request);
}
