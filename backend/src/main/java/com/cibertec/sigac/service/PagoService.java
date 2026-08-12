package com.cibertec.sigac.service;

import java.time.LocalDate;
import java.util.List;

import com.cibertec.sigac.dto.CanjeBancarioRequest;
import com.cibertec.sigac.dto.IngresoExternoRequest;
import com.cibertec.sigac.dto.ProcesarPagoRequest;
import com.cibertec.sigac.dto.ProcesarPagoResponse;
import com.cibertec.sigac.dto.ReciboResponse;
import com.cibertec.sigac.entity.TipoRecibo;

public interface PagoService {

    ProcesarPagoResponse procesarPagoCuentas(ProcesarPagoRequest request);

    ReciboResponse canjeBancario(CanjeBancarioRequest request);

    ReciboResponse registrarIngresoExterno(IngresoExternoRequest request);

    List<ReciboResponse> listarRecibos(LocalDate fecha, TipoRecibo tipo);

    ReciboResponse obtenerReciboPorId(Long id);
}
