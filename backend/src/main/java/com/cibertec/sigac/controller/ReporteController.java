package com.cibertec.sigac.controller;

import java.time.LocalDate;
import java.util.function.BiFunction;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cibertec.sigac.service.ReporteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteService reporteService;

    @GetMapping("/movimientos")
    public ResponseEntity<byte[]> movimientos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descargar("movimientos", desde, hasta, reporteService::generarMovimientos);
    }

    @GetMapping("/totales")
    public ResponseEntity<byte[]> totales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descargar("totales", desde, hasta, reporteService::generarTotales);
    }

    @GetMapping("/socios")
    public ResponseEntity<byte[]> socios(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descargar("socios", desde, hasta, reporteService::generarSocios);
    }

    @GetMapping("/no-socios")
    public ResponseEntity<byte[]> noSocios(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descargar("no-socios", desde, hasta, reporteService::generarNoSocios);
    }

    @GetMapping("/egresos")
    public ResponseEntity<byte[]> egresos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descargar("egresos", desde, hasta, reporteService::generarEgresos);
    }

    @GetMapping("/bancos")
    public ResponseEntity<byte[]> bancos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descargar("bancos", desde, hasta, reporteService::generarBancos);
    }

    private ResponseEntity<byte[]> descargar(
            String nombre, LocalDate desde, LocalDate hasta, BiFunction<LocalDate, LocalDate, byte[]> generador) {
        byte[] contenido = generador.apply(desde, hasta);
        String archivo = "reporte-%s-%s_a_%s.xlsx".formatted(nombre, desde, hasta);

        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo + "\"")
                .body(contenido);
    }
}
