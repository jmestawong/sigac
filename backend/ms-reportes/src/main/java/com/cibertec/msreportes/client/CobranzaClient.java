package com.cibertec.msreportes.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.cibertec.msreportes.dto.ReciboResponse;

@FeignClient(name = "ms-cobranza")
public interface CobranzaClient {

    @GetMapping("/api/recibos")
    List<ReciboResponse> listarRecibos();
}
