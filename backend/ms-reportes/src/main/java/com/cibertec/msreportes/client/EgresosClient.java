package com.cibertec.msreportes.client;

import java.time.LocalDate;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.cibertec.msreportes.dto.EgresoResponse;

@FeignClient(name = "ms-egresos")
public interface EgresosClient {

    @GetMapping("/api/egresos")
    List<EgresoResponse> listarPorRango(@RequestParam("desde") LocalDate desde, @RequestParam("hasta") LocalDate hasta);
}
