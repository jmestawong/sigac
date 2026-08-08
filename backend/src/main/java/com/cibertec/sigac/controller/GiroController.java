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

import com.cibertec.sigac.dto.GiroRequest;
import com.cibertec.sigac.dto.GiroResponse;
import com.cibertec.sigac.service.GiroService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/giros")
@RequiredArgsConstructor
public class GiroController {

    private final GiroService giroService;

    @GetMapping
    public List<GiroResponse> listar() {
        return giroService.listarTodos();
    }

    @GetMapping("/{id}")
    public GiroResponse obtener(@PathVariable Long id) {
        return giroService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GiroResponse crear(@Valid @RequestBody GiroRequest request) {
        return giroService.crear(request);
    }

    @PutMapping("/{id}")
    public GiroResponse actualizar(@PathVariable Long id, @Valid @RequestBody GiroRequest request) {
        return giroService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        giroService.eliminar(id);
    }
}
