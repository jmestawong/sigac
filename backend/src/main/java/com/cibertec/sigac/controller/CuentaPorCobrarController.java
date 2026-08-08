package com.cibertec.sigac.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cibertec.sigac.dto.CuentaPorCobrarResponse;
import com.cibertec.sigac.dto.GenerarPuestosConsumoRequest;
import com.cibertec.sigac.dto.GenerarPuestosMontoFijoRequest;
import com.cibertec.sigac.dto.GenerarSociosRequest;
import com.cibertec.sigac.dto.ResumenPuestoResponse;
import com.cibertec.sigac.dto.ResumenSocioResponse;
import com.cibertec.sigac.service.CuentaPorCobrarService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cuentas-por-cobrar")
@RequiredArgsConstructor
public class CuentaPorCobrarController {

    private final CuentaPorCobrarService cuentaPorCobrarService;

    @GetMapping
    public List<CuentaPorCobrarResponse> listar() {
        return cuentaPorCobrarService.listarTodas();
    }

    @GetMapping("/{id}")
    public CuentaPorCobrarResponse obtener(@PathVariable Long id) {
        return cuentaPorCobrarService.obtenerPorId(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        cuentaPorCobrarService.eliminar(id);
    }

    @PostMapping("/generar/puestos-monto-fijo")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CuentaPorCobrarResponse> generarParaPuestosMontoFijo(
            @Valid @RequestBody GenerarPuestosMontoFijoRequest request) {
        return cuentaPorCobrarService.generarParaPuestosMontoFijo(request);
    }

    @PostMapping("/generar/puestos-consumo")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CuentaPorCobrarResponse> generarParaPuestosConsumo(
            @Valid @RequestBody GenerarPuestosConsumoRequest request) {
        return cuentaPorCobrarService.generarParaPuestosConsumo(request);
    }

    @PostMapping("/generar/socios")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CuentaPorCobrarResponse> generarParaSocios(@Valid @RequestBody GenerarSociosRequest request) {
        return cuentaPorCobrarService.generarParaSocios(request);
    }

    @GetMapping("/resumen/socio/{socioId}")
    public ResumenSocioResponse resumenPorSocio(@PathVariable Long socioId) {
        return cuentaPorCobrarService.resumenPorSocio(socioId);
    }

    @GetMapping("/resumen/puesto/{puestoId}")
    public ResumenPuestoResponse resumenPorPuesto(@PathVariable Long puestoId) {
        return cuentaPorCobrarService.resumenPorPuesto(puestoId);
    }
}
