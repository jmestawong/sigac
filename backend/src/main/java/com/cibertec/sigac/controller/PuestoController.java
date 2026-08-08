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

import com.cibertec.sigac.dto.PuestoRequest;
import com.cibertec.sigac.dto.PuestoResponse;
import com.cibertec.sigac.service.PuestoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/puestos")
@RequiredArgsConstructor
public class PuestoController {

    private final PuestoService puestoService;

    @GetMapping
    public List<PuestoResponse> listar() {
        return puestoService.listarTodos();
    }

    @GetMapping("/{id}")
    public PuestoResponse obtener(@PathVariable Long id) {
        return puestoService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PuestoResponse crear(@Valid @RequestBody PuestoRequest request) {
        return puestoService.crear(request);
    }

    @PutMapping("/{id}")
    public PuestoResponse actualizar(@PathVariable Long id, @Valid @RequestBody PuestoRequest request) {
        return puestoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        puestoService.eliminar(id);
    }
}
