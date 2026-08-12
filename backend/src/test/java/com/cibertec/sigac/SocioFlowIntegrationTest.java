package com.cibertec.sigac;

import static org.hamcrest.Matchers.is;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba de integracion end-to-end: login (POST /api/auth/login) y CRUD completo
 * de /api/socios contra una base de datos H2 en memoria, verificando que las
 * rutas protegidas exigen JWT y que las operaciones persisten los cambios.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SocioFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void flujoCompleto_loginYCrudDeSocios() throws Exception {
        // Sin token -> rechazado
        mockMvc.perform(get("/api/socios"))
                .andExpect(status().isUnauthorized());

        // Login con el usuario admin sembrado por DataSeeder
        String loginBody = """
                {"username":"admin","password":"admin123"}
                """;

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("ADMIN")))
                .andReturn().getResponse().getContentAsString();

        String token = loginResponse.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        String authHeader = "Bearer " + token;

        // Crear socio
        String crearBody = """
                {"codigo":"IT-001","nombres":"Ana","apellidos":"Torres","accion":"Ordinaria","etapa":"Activo","fechaNacimiento":"1992-04-10"}
                """;

        String crearResponse = mockMvc.perform(post("/api/socios")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo", is("IT-001")))
                .andReturn().getResponse().getContentAsString();

        Long id = Long.valueOf(crearResponse.replaceAll(".*\"id\":(\\d+).*", "$1"));

        // Listar
        mockMvc.perform(get("/api/socios").header("Authorization", authHeader))
                .andExpect(status().isOk());

        // Actualizar
        String actualizarBody = """
                {"codigo":"IT-001","nombres":"Ana Maria","apellidos":"Torres","accion":"Ordinaria","etapa":"Suspendido","fechaNacimiento":"1992-04-10"}
                """;

        mockMvc.perform(put("/api/socios/" + id)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actualizarBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa", is("Suspendido")));

        // Eliminar
        mockMvc.perform(delete("/api/socios/" + id).header("Authorization", authHeader))
                .andExpect(status().isNoContent());

        // Verificar que ya no existe
        mockMvc.perform(get("/api/socios/" + id).header("Authorization", authHeader))
                .andExpect(status().isNotFound());
    }

    @Test
    void operador_puedeConsultarSociosPeroNoEscribirCatalogos() throws Exception {
        String loginBody = """
                {"username":"operador","password":"operador123"}
                """;

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("OPERADOR")))
                .andReturn().getResponse().getContentAsString();

        String token = loginResponse.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        String authHeader = "Bearer " + token;

        // Puede leer el catalogo de socios
        mockMvc.perform(get("/api/socios").header("Authorization", authHeader))
                .andExpect(status().isOk());

        // No puede crear, actualizar ni eliminar socios (catalogo exclusivo de ADMIN)
        String crearBody = """
                {"codigo":"OP-001","nombres":"X","apellidos":"Y","accion":"Ordinaria","etapa":"1","fechaNacimiento":"1992-04-10"}
                """;

        mockMvc.perform(post("/api/socios")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/socios/1")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/socios/1").header("Authorization", authHeader))
                .andExpect(status().isForbidden());
    }
}
