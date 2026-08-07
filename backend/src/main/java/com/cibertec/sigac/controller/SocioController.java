package com.cibertec.sigac.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cibertec.sigac.dto.SocioRequest;
import com.cibertec.sigac.dto.SocioResponse;
import com.cibertec.sigac.service.SocioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/socios")
@RequiredArgsConstructor
public class SocioController {

    private final SocioService socioService;

    @GetMapping
    public List<SocioResponse> listar() {
        return socioService.listarTodos();
    }

    @GetMapping("/{id}")
    public SocioResponse obtener(@PathVariable Long id) {
        return socioService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SocioResponse crear(@Valid @RequestBody SocioRequest request) {
        return socioService.crear(request);
    }

    @PutMapping("/{id}")
    public SocioResponse actualizar(@PathVariable Long id, @Valid @RequestBody SocioRequest request) {
        return socioService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        socioService.eliminar(id);
    }
}
