package com.cibertec.sigac.service;

import java.time.LocalDate;

public interface ReporteService {

    byte[] generarMovimientos(LocalDate desde, LocalDate hasta);

    byte[] generarTotales(LocalDate desde, LocalDate hasta);

    byte[] generarSocios(LocalDate desde, LocalDate hasta);

    byte[] generarNoSocios(LocalDate desde, LocalDate hasta);

    byte[] generarEgresos(LocalDate desde, LocalDate hasta);

    byte[] generarBancos(LocalDate desde, LocalDate hasta);
}
