package com.cibertec.sigac.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cibertec.sigac.dto.ReciboResponse;
import com.cibertec.sigac.service.PagoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recibos")
@RequiredArgsConstructor
public class ReciboController {

    private final PagoService pagoService;

    @GetMapping
    public List<ReciboResponse> listar() {
        return pagoService.listarRecibos();
    }
}
