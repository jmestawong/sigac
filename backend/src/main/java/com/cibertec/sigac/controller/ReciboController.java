package com.cibertec.sigac.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cibertec.sigac.dto.ReciboResponse;
import com.cibertec.sigac.entity.TipoRecibo;
import com.cibertec.sigac.service.PagoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recibos")
@RequiredArgsConstructor
public class ReciboController {

    private final PagoService pagoService;

    @GetMapping
    public List<ReciboResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) TipoRecibo tipo) {
        return pagoService.listarRecibos(fecha, tipo);
    }

    @GetMapping("/{id}")
    public ReciboResponse obtener(@PathVariable Long id) {
        return pagoService.obtenerReciboPorId(id);
    }
}
