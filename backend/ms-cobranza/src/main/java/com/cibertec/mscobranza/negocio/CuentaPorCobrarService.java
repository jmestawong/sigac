package com.cibertec.mscobranza.negocio;

import java.util.List;

import com.cibertec.mscobranza.dto.CuentaPorCobrarResponse;
import com.cibertec.mscobranza.dto.GenerarPuestosConsumoRequest;
import com.cibertec.mscobranza.dto.GenerarPuestosMontoFijoRequest;
import com.cibertec.mscobranza.dto.GenerarSociosRequest;
import com.cibertec.mscobranza.dto.ReferenciaResponse;
import com.cibertec.mscobranza.dto.ResumenPuestoResponse;
import com.cibertec.mscobranza.dto.ResumenSocioResponse;

public interface CuentaPorCobrarService {

    List<CuentaPorCobrarResponse> listarTodas();

    CuentaPorCobrarResponse obtenerPorId(Long id);

    void eliminar(Long id);

    List<CuentaPorCobrarResponse> generarParaPuestosMontoFijo(GenerarPuestosMontoFijoRequest request);

    List<CuentaPorCobrarResponse> generarParaPuestosConsumo(GenerarPuestosConsumoRequest request);

    List<CuentaPorCobrarResponse> generarParaSocios(GenerarSociosRequest request);

    ResumenSocioResponse resumenPorSocio(Long socioId);

    ResumenPuestoResponse resumenPorPuesto(Long puestoId);

    ReferenciaResponse referencias(String tipo, Long id);
}
