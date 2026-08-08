package com.cibertec.sigac.service;

import java.util.List;

import com.cibertec.sigac.dto.CanjeBancarioRequest;
import com.cibertec.sigac.dto.IngresoExternoRequest;
import com.cibertec.sigac.dto.ProcesarPagoRequest;
import com.cibertec.sigac.dto.ProcesarPagoResponse;
import com.cibertec.sigac.dto.ReciboResponse;

public interface PagoService {

    ProcesarPagoResponse procesarPagoCuentas(ProcesarPagoRequest request);

    ReciboResponse canjeBancario(CanjeBancarioRequest request);

    ReciboResponse registrarIngresoExterno(IngresoExternoRequest request);

    List<ReciboResponse> listarRecibos();
}
