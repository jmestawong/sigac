package com.cibertec.sigac.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cibertec.sigac.dto.CargaMasivaEgresosResponse;
import com.cibertec.sigac.dto.EgresoRequest;
import com.cibertec.sigac.dto.EgresoResponse;
import com.cibertec.sigac.service.EgresoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/egresos")
@RequiredArgsConstructor
public class EgresoController {

    private final EgresoService egresoService;

    @GetMapping
    public List<EgresoResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (desde != null && hasta != null) {
            return egresoService.listarPorRango(desde, hasta);
        }
        return egresoService.listarTodos();
    }

    @GetMapping("/{id}")
    public EgresoResponse obtener(@PathVariable Long id) {
        return egresoService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EgresoResponse registrar(@Valid @RequestBody EgresoRequest request) {
        return egresoService.registrar(request);
    }

    @PostMapping("/{id}/procesar")
    public EgresoResponse procesar(@PathVariable Long id) {
        return egresoService.procesar(id);
    }

    @PostMapping("/{id}/anular")
    public EgresoResponse anular(@PathVariable Long id) {
        return egresoService.anular(id);
    }

    @PostMapping("/carga-masiva")
    @ResponseStatus(HttpStatus.CREATED)
    public CargaMasivaEgresosResponse cargaMasiva(@RequestParam("archivo") MultipartFile archivo) {
        return egresoService.registrarMasivo(archivo);
    }
}
