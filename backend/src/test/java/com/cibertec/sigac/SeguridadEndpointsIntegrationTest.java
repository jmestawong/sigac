package com.cibertec.sigac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Verifica la seguridad de TODOS los endpoints REST del sistema, descubiertos
 * automaticamente desde los controladores:
 *  - todos exigen un token JWT valido, excepto el login (servicio publico);
 *  - el rol OPERADOR puede consultar todo, pero no puede escribir en los
 *    catalogos ni generar o eliminar cuentas por cobrar (solo ADMIN).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadEndpointsIntegrationTest {

    private static final String LOGIN = "/api/auth/login";
    private static final List<String> CATALOGOS_ADMIN = List.of(
            "/api/socios", "/api/puestos", "/api/giros", "/api/servicios", "/api/bancos");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    private record Endpoint(HttpMethod metodo, String ruta) {
        @Override
        public String toString() {
            return metodo + " " + ruta;
        }
    }

    private List<Endpoint> descubrirEndpoints() {
        List<Endpoint> endpoints = new ArrayList<>();
        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
            if (!handler.getBeanType().getPackageName().equals("com.cibertec.sigac.controller")) {
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

    private String tokenDe(String username, String password) throws Exception {
        String respuesta = mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + respuesta.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    private int statusDe(Endpoint endpoint, String authorization) throws Exception {
        var peticion = request(endpoint.metodo(), endpoint.ruta());
        if (authorization != null) {
            peticion.header("Authorization", authorization);
        }
        return mockMvc.perform(peticion).andReturn().getResponse().getStatus();
    }

    private boolean esEscrituraDeAdmin(Endpoint endpoint) {
        boolean escritura = endpoint.metodo() != HttpMethod.GET;
        boolean catalogo = CATALOGOS_ADMIN.stream().anyMatch(endpoint.ruta()::startsWith);
        boolean cuentas = endpoint.ruta().startsWith("/api/cuentas-por-cobrar")
                && (endpoint.metodo() == HttpMethod.DELETE || endpoint.ruta().contains("/generar/"));
        return escritura && (catalogo || cuentas);
    }

    @Test
    void descubreLosEndpointsDeLosControladores() {
        List<Endpoint> endpoints = descubrirEndpoints();

        assertThat(endpoints).hasSizeGreaterThanOrEqualTo(50);
        assertThat(endpoints).extracting(Endpoint::metodo)
                .contains(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE);
    }

    @Test
    void todosLosEndpointsDeNegocioRechazanLasPeticionesSinToken() throws Exception {
        List<String> sinProteger = new ArrayList<>();
        int protegidos = 0;

        for (Endpoint endpoint : descubrirEndpoints()) {
            if (endpoint.ruta().equals(LOGIN)) {
                continue;
            }
            int status = statusDe(endpoint, null);
            if (status == 401) {
                protegidos++;
            } else {
                sinProteger.add(endpoint + " respondio " + status);
            }
        }

        assertThat(sinProteger).as("endpoints que no exigen autenticacion").isEmpty();
        assertThat(protegidos).as("endpoints de negocio protegidos").isGreaterThanOrEqualTo(50);
    }

    @Test
    void unTokenInvalidoEsRechazadoEnTodosLosEndpointsDeNegocio() throws Exception {
        List<String> aceptados = new ArrayList<>();

        for (Endpoint endpoint : descubrirEndpoints()) {
            if (endpoint.ruta().equals(LOGIN)) {
                continue;
            }
            int status = statusDe(endpoint, "Bearer esto.no.es.un.jwt");
            if (status != 401) {
                aceptados.add(endpoint + " respondio " + status);
            }
        }

        assertThat(aceptados).isEmpty();
    }

    @Test
    void elOperadorNuncaRecibe403AlConsultarYSi403AlEscribirEnLoReservadoAlAdmin() throws Exception {
        String operador = tokenDe("operador", "operador123");
        List<String> incorrectos = new ArrayList<>();

        for (Endpoint endpoint : descubrirEndpoints()) {
            if (endpoint.ruta().equals(LOGIN)) {
                continue;
            }
            int status = statusDe(endpoint, operador);
            boolean reservado = esEscrituraDeAdmin(endpoint);

            if (reservado && status != 403) {
                incorrectos.add(endpoint + " deberia dar 403 al operador y dio " + status);
            }
            if (!reservado && (status == 401 || status == 403)) {
                incorrectos.add(endpoint + " deberia estar permitido al operador y dio " + status);
            }
        }

        assertThat(incorrectos).isEmpty();
    }

    @Test
    void elAdministradorNoRecibeNi401Ni403EnNingunEndpoint() throws Exception {
        String admin = tokenDe("admin", "admin123");
        List<String> bloqueados = new ArrayList<>();

        for (Endpoint endpoint : descubrirEndpoints()) {
            if (endpoint.ruta().equals(LOGIN)) {
                continue;
            }
            int status = statusDe(endpoint, admin);
            if (status == 401 || status == 403) {
                bloqueados.add(endpoint + " respondio " + status);
            }
        }

        assertThat(bloqueados).isEmpty();
    }
}
