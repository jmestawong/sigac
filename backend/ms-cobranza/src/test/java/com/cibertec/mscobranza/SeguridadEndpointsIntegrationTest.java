package com.cibertec.mscobranza;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.cibertec.mscobranza.client.CatalogosClient;
import com.cibertec.sigac.common.security.JwtService;

/**
 * Seguridad de TODOS los endpoints de ms-cobranza (descubiertos desde los
 * controladores): exigen JWT valido; el OPERADOR consulta y cobra, pero no
 * puede generar ni eliminar cuentas por cobrar (solo ADMIN).
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

    @MockitoBean
    private CatalogosClient catalogosClient;

    @BeforeEach
    void catalogos() {
        CobranzaTestSupport.configurarCatalogos(catalogosClient);
    }

    private record Endpoint(HttpMethod metodo, String ruta) {
        @Override
        public String toString() {
            return metodo + " " + ruta;
        }
    }

    private List<Endpoint> descubrirEndpoints() {
        List<Endpoint> endpoints = new ArrayList<>();
        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
            if (!handler.getBeanType().getPackageName().equals("com.cibertec.mscobranza.rest")) {
                return;
            }
            Set<String> rutas = info.getPathPatternsCondition().getPatternValues();
            Set<RequestMethod> metodos = info.getMethodsCondition().getMethods();
            for (String ruta : rutas) {
                for (RequestMethod metodo : metodos) {
                    endpoints.add(new Endpoint(HttpMethod.valueOf(metodo.name()),
                            ruta.replaceAll("\\{tipo}", "socio").replaceAll("\\{[^}]+}", "1")));
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

    private boolean reservadoAlAdmin(Endpoint endpoint) {
        return endpoint.metodo() == HttpMethod.DELETE
                || (endpoint.metodo() == HttpMethod.POST && endpoint.ruta().contains("/generar/"));
    }

    @Test
    void descubreLos14EndpointsDeCobranza() {
        assertThat(descubrirEndpoints()).hasSize(14);
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
    void elOperadorNoPuedeGenerarNiEliminarPeroSiConsultarYCobrar() throws Exception {
        String operador = "Bearer " + jwtService.generarToken("operador", "OPERADOR");
        List<String> incorrectos = new ArrayList<>();

        for (Endpoint endpoint : descubrirEndpoints()) {
            int status = statusDe(endpoint, operador);
            if (reservadoAlAdmin(endpoint) && status != 403) {
                incorrectos.add(endpoint + " deberia dar 403 al operador y dio " + status);
            }
            if (!reservadoAlAdmin(endpoint) && (status == 401 || status == 403)) {
                incorrectos.add(endpoint + " deberia estar permitido al operador y dio " + status);
            }
        }
        assertThat(incorrectos).isEmpty();
    }

    @Test
    void elAdministradorNoRecibeNi401Ni403EnNingunEndpoint() throws Exception {
        String admin = "Bearer " + jwtService.generarToken("admin", "ADMIN");
        List<String> bloqueados = new ArrayList<>();

        for (Endpoint endpoint : descubrirEndpoints()) {
            int status = statusDe(endpoint, admin);
            if (status == 401 || status == 403) {
                bloqueados.add(endpoint + " respondio " + status);
            }
        }
        assertThat(bloqueados).isEmpty();
    }
}
