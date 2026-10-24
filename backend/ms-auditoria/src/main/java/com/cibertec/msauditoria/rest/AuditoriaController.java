package com.cibertec.msauditoria.rest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cibertec.msauditoria.entidades.RegistroAuditoria;
import com.cibertec.msauditoria.negocio.AuditoriaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    /** Bitacora de movimientos, de la mas reciente a la mas antigua; filtrable por modulo o por usuario. */
    @GetMapping
    public List<RegistroAuditoria> listar(@RequestParam(required = false) String modulo,
            @RequestParam(required = false) String usuario) {
        return auditoriaService.listar(modulo, usuario);
    }
}
