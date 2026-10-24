package com.cibertec.mscatalogos.rest;

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

import com.cibertec.mscatalogos.dto.BancoRequest;
import com.cibertec.mscatalogos.dto.BancoResponse;
import com.cibertec.mscatalogos.negocio.BancoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bancos")
@RequiredArgsConstructor
public class BancoController {

    private final BancoService bancoService;

    @GetMapping
    public List<BancoResponse> listar() {
        return bancoService.listarTodos();
    }

    @GetMapping("/{id}")
    public BancoResponse obtener(@PathVariable Long id) {
        return bancoService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BancoResponse crear(@Valid @RequestBody BancoRequest request) {
        return bancoService.crear(request);
    }

    @PutMapping("/{id}")
    public BancoResponse actualizar(@PathVariable Long id, @Valid @RequestBody BancoRequest request) {
        return bancoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        bancoService.eliminar(id);
    }
}
