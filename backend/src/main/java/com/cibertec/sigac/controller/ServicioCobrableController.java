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

import com.cibertec.sigac.dto.ServicioCobrableRequest;
import com.cibertec.sigac.dto.ServicioCobrableResponse;
import com.cibertec.sigac.service.ServicioCobrableService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
public class ServicioCobrableController {

    private final ServicioCobrableService servicioCobrableService;

    @GetMapping
    public List<ServicioCobrableResponse> listar() {
        return servicioCobrableService.listarTodos();
    }

    @GetMapping("/{id}")
    public ServicioCobrableResponse obtener(@PathVariable Long id) {
        return servicioCobrableService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicioCobrableResponse crear(@Valid @RequestBody ServicioCobrableRequest request) {
        return servicioCobrableService.crear(request);
    }

    @PutMapping("/{id}")
    public ServicioCobrableResponse actualizar(@PathVariable Long id, @Valid @RequestBody ServicioCobrableRequest request) {
        return servicioCobrableService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicioCobrableService.eliminar(id);
    }
}
