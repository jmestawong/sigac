package com.cibertec.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

@SpringBootTest(properties = { "eureka.client.enabled=false", "spring.cloud.discovery.enabled=false" })
class RutasGatewayTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void existenLasSeisRutasHaciaLosMicroservicios() {
        List<Route> rutas = routeLocator.getRoutes().collectList().block();

        Map<String, String> destinos = rutas.stream()
                .collect(Collectors.toMap(Route::getId, r -> r.getUri().toString()));

        assertThat(destinos).containsOnlyKeys("ms-auth", "ms-catalogos", "ms-cobranza", "ms-egresos", "ms-reportes",
                "ms-auditoria");
        assertThat(destinos).containsEntry("ms-auth", "lb://ms-auth")
                .containsEntry("ms-catalogos", "lb://ms-catalogos")
                .containsEntry("ms-cobranza", "lb://ms-cobranza")
                .containsEntry("ms-egresos", "lb://ms-egresos")
                .containsEntry("ms-reportes", "lb://ms-reportes")
                .containsEntry("ms-auditoria", "lb://ms-auditoria");
    }
}
