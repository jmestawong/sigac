package com.cibertec.mscobranza.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cibertec.mscobranza.dto.BancoResponse;
import com.cibertec.mscobranza.dto.PuestoResponse;
import com.cibertec.mscobranza.dto.ServicioCobrableResponse;
import com.cibertec.mscobranza.dto.SocioResponse;

@FeignClient(name = "ms-catalogos")
public interface CatalogosClient {

    @GetMapping("/api/socios")
    List<SocioResponse> listarSocios();

    @GetMapping("/api/socios/{id}")
    SocioResponse obtenerSocio(@PathVariable("id") Long id);

    @GetMapping("/api/puestos")
    List<PuestoResponse> listarPuestos();

    @GetMapping("/api/puestos/{id}")
    PuestoResponse obtenerPuesto(@PathVariable("id") Long id);

    @GetMapping("/api/servicios/{id}")
    ServicioCobrableResponse obtenerServicio(@PathVariable("id") Long id);

    @GetMapping("/api/bancos/{id}")
    BancoResponse obtenerBanco(@PathVariable("id") Long id);
}
