package com.cibertec.sigac.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cibertec.sigac.dto.CanjeBancarioRequest;
import com.cibertec.sigac.dto.IngresoExternoRequest;
import com.cibertec.sigac.dto.ProcesarPagoRequest;
import com.cibertec.sigac.dto.ProcesarPagoResponse;
import com.cibertec.sigac.dto.ReciboResponse;
import com.cibertec.sigac.service.PagoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/procesar-cuentas")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcesarPagoResponse procesarPagoCuentas(@Valid @RequestBody ProcesarPagoRequest request) {
        return pagoService.procesarPagoCuentas(request);
    }

    @PostMapping("/canje-bancario")
    @ResponseStatus(HttpStatus.CREATED)
    public ReciboResponse canjeBancario(@Valid @RequestBody CanjeBancarioRequest request) {
        return pagoService.canjeBancario(request);
    }

    @PostMapping("/ingreso-externo")
    @ResponseStatus(HttpStatus.CREATED)
    public ReciboResponse registrarIngresoExterno(@Valid @RequestBody IngresoExternoRequest request) {
        return pagoService.registrarIngresoExterno(request);
    }
}
