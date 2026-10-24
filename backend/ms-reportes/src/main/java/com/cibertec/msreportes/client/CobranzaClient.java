package com.cibertec.msreportes.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.cibertec.msreportes.dto.ReciboResponse;

/** Cliente OpenFeign de ms-cobranza: los recibos ya traen sus cuentas, socios, puestos y servicios. */
@FeignClient(name = "ms-cobranza")
public interface CobranzaClient {

    @GetMapping("/api/recibos")
    List<ReciboResponse> listarRecibos();
}
