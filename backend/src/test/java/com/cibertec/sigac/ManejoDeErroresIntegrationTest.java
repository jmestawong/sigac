package com.cibertec.sigac;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica que los errores originados en la propia peticion HTTP se informen
 * con su codigo 4xx correcto y el formato ErrorResponse, en lugar de un 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ManejoDeErroresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String authHeader;

    @BeforeEach
    void iniciarSesion() throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andReturn().getResponse().getContentAsString();
        authHeader = "Bearer " + respuesta.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void cuerpoJsonMalFormado_retorna400() throws Exception {
        mockMvc.perform(post("/api/giros")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void reporteSinParametrosObligatorios_retorna400IndicandoCualFalta() throws Exception {
        mockMvc.perform(get("/api/reportes/totales").header("Authorization", authHeader))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("desde")));
    }

    @Test
    void reporteConFechaInvalida_retorna400() throws Exception {
        mockMvc.perform(get("/api/reportes/totales")
                        .param("desde", "no-es-fecha")
                        .param("hasta", "2026-12-31")
                        .header("Authorization", authHeader))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("desde")));
    }

    @Test
    void metodoNoSoportado_retorna405() throws Exception {
        mockMvc.perform(delete("/api/auth/login").header("Authorization", authHeader))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void cargaMasivaSinArchivo_retorna400() throws Exception {
        mockMvc.perform(post("/api/egresos/carga-masiva")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header("Authorization", authHeader))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recursoInexistente_retorna404ConFormatoErrorResponse() throws Exception {
        mockMvc.perform(get("/api/giros/999999").header("Authorization", authHeader))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
}
