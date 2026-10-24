package com.cibertec.mscatalogos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cibertec.mscatalogos.client.CobranzaClient;
import com.cibertec.mscatalogos.dto.ReferenciaResponse;
import com.cibertec.sigac.common.security.JwtService;

/**
 * Flujo completo del microservicio de catalogos: JWT emitido por ms-auth
 * (aqui simulado con el mismo JwtService), CRUD de socios sobre H2 y reglas de
 * rol. ms-cobranza se reemplaza por un mock del cliente OpenFeign.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogosFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CobranzaClient cobranzaClient;

    private String bearer(String usuario, String rol) {
        return "Bearer " + jwtService.generarToken(usuario, rol);
    }

    @Test
    void flujoCompleto_crudDeSocios() throws Exception {
        when(cobranzaClient.referencias(eq("socio"), anyLong()))
                .thenReturn(ReferenciaResponse.builder().tipo("socio").id(1L).cantidad(0).build());

        mockMvc.perform(get("/api/socios")).andExpect(status().isUnauthorized());

        String admin = bearer("admin", "ADMIN");
        String crear = """
                {"codigo":"IT-001","nombres":"Ana","apellidos":"Torres","accion":"Ordinaria","etapa":"1","fechaNacimiento":"1992-04-10"}
                """;
        String creado = mockMvc.perform(post("/api/socios").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(crear))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo", is("IT-001")))
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(creado.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/socios").header("Authorization", admin)).andExpect(status().isOk());

        String actualizar = """
                {"codigo":"IT-001","nombres":"Ana Maria","apellidos":"Torres","accion":"Ordinaria","etapa":"2","fechaNacimiento":"1992-04-10"}
                """;
        mockMvc.perform(put("/api/socios/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(actualizar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa", is("2")));

        mockMvc.perform(delete("/api/socios/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/socios/" + id).header("Authorization", admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void operador_puedeConsultarPeroNoEscribirCatalogos() throws Exception {
        String operador = bearer("operador", "OPERADOR");
        String cuerpo = """
                {"codigo":"OP-001","nombres":"X","apellidos":"Y","accion":"Ordinaria","etapa":"1","fechaNacimiento":"1992-04-10"}
                """;

        mockMvc.perform(get("/api/socios").header("Authorization", operador)).andExpect(status().isOk());
        mockMvc.perform(post("/api/socios").header("Authorization", operador)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/socios/1").header("Authorization", operador)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/socios/1").header("Authorization", operador))
                .andExpect(status().isForbidden());
    }

    @Test
    void noSePuedeEliminarUnSocioConCobranzaAsociada() throws Exception {
        when(cobranzaClient.referencias(eq("socio"), anyLong()))
                .thenReturn(ReferenciaResponse.builder().tipo("socio").id(1L).cantidad(3).build());

        String admin = bearer("admin", "ADMIN");
        String crear = """
                {"codigo":"IT-002","nombres":"Luis","apellidos":"Rojas","accion":"Ordinaria","etapa":"1","fechaNacimiento":"1990-01-20"}
                """;
        String creado = mockMvc.perform(post("/api/socios").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(crear))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(creado.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(delete("/api/socios/" + id).header("Authorization", admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("registro(s) de cobranza")));

        mockMvc.perform(get("/api/socios/" + id).header("Authorization", admin))
                .andExpect(status().isOk());
    }
}
