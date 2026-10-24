package com.cibertec.mscatalogos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cibertec.mscatalogos.dto.ReferenciaResponse;

/** Cliente OpenFeign de ms-cobranza (descubierto por Eureka como "ms-cobranza"). */
@FeignClient(name = "ms-cobranza")
public interface CobranzaClient {

    @GetMapping("/api/cuentas-por-cobrar/referencias/{tipo}/{id}")
    ReferenciaResponse referencias(@PathVariable("tipo") String tipo, @PathVariable("id") Long id);
}
