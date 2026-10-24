package com.cibertec.mscobranza.negocio;

import java.time.LocalDate;
import java.util.List;

import com.cibertec.mscobranza.dto.CanjeBancarioRequest;
import com.cibertec.mscobranza.dto.IngresoExternoRequest;
import com.cibertec.mscobranza.dto.ProcesarPagoRequest;
import com.cibertec.mscobranza.dto.ProcesarPagoResponse;
import com.cibertec.mscobranza.dto.ReciboResponse;
import com.cibertec.mscobranza.entidades.TipoRecibo;

public interface PagoService {

    ProcesarPagoResponse procesarPagoCuentas(ProcesarPagoRequest request);

    ReciboResponse canjeBancario(CanjeBancarioRequest request);

    ReciboResponse registrarIngresoExterno(IngresoExternoRequest request);

    List<ReciboResponse> listarRecibos(LocalDate fecha, TipoRecibo tipo);

    ReciboResponse obtenerReciboPorId(Long id);
}
