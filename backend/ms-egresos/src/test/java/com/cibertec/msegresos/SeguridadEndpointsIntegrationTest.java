package com.cibertec.msegresos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.cibertec.sigac.common.security.JwtService;

/**
 * Seguridad de TODOS los endpoints de ms-egresos (descubiertos desde los
 * controladores): exigen JWT valido; tanto ADMIN como OPERADOR pueden operarlos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadEndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private JwtService jwtService;

    private record Endpoint(HttpMethod metodo, String ruta) {
        @Override
        public String toString() {
            return metodo + " " + ruta;
        }
    }

    private List<Endpoint> descubrirEndpoints() {
        List<Endpoint> endpoints = new ArrayList<>();
        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
            if (!handler.getBeanType().getPackageName().equals("com.cibertec.msegresos.rest")) {
                return;
            }
            Set<String> rutas = info.getPathPatternsCondition().getPatternValues();
            Set<RequestMethod> metodos = info.getMethodsCondition().getMethods();
            for (String ruta : rutas) {
                for (RequestMethod metodo : metodos) {
                    endpoints.add(new Endpoint(HttpMethod.valueOf(metodo.name()), ruta.replaceAll("\\{[^}]+}", "1")));
                }
            }
        });
        return endpoints;
    }

    private int statusDe(Endpoint endpoint, String authorization) throws Exception {
        var peticion = request(endpoint.metodo(), endpoint.ruta());
        if (authorization != null) {
            peticion.header("Authorization", authorization);
        }
        return mockMvc.perform(peticion).andReturn().getResponse().getStatus();
    }

    @Test
    void descubreLos6EndpointsDeEgresos() {
        assertThat(descubrirEndpoints()).hasSize(6);
    }

    @Test
    void todosLosEndpointsRechazanPeticionesSinTokenOConTokenInvalido() throws Exception {
        List<String> sinProteger = new ArrayList<>();
        for (Endpoint endpoint : descubrirEndpoints()) {
            if (statusDe(endpoint, null) != 401) {
                sinProteger.add(endpoint + " sin token");
            }
            if (statusDe(endpoint, "Bearer esto.no.es.un.jwt") != 401) {
                sinProteger.add(endpoint + " con token invalido");
            }
        }
        assertThat(sinProteger).isEmpty();
    }

    @Test
    void adminYOperadorNoRecibenNi401Ni403() throws Exception {
        List<String> bloqueados = new ArrayList<>();
        for (String rol : List.of("ADMIN", "OPERADOR")) {
            String token = "Bearer " + jwtService.generarToken(rol.toLowerCase(), rol);
            for (Endpoint endpoint : descubrirEndpoints()) {
                int status = statusDe(endpoint, token);
                if (status == 401 || status == 403) {
                    bloqueados.add(rol + " " + endpoint + " respondio " + status);
                }
            }
        }
        assertThat(bloqueados).isEmpty();
    }
}
