package com.cibertec.mscatalogos;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.cibertec.sigac.common.security.JwtService;

/**
 * Los errores originados en la propia peticion HTTP se informan con su codigo
 * 4xx y el formato ErrorResponse, en lugar de un 500 generico.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ManejoDeErroresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CobranzaClient cobranzaClient;

    private String admin() {
        return "Bearer " + jwtService.generarToken("admin", "ADMIN");
    }

    @Test
    void cuerpoJsonMalFormado_retorna400() throws Exception {
        mockMvc.perform(post("/api/giros").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void validacionDeCampos_retorna400ConElDetalle() throws Exception {
        mockMvc.perform(post("/api/giros").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Error de validacion")));
    }

    @Test
    void metodoNoSoportado_retorna405() throws Exception {
        mockMvc.perform(patch("/api/giros/1").header("Authorization", admin()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void recursoInexistente_retorna404ConFormatoErrorResponse() throws Exception {
        mockMvc.perform(get("/api/giros/999999").header("Authorization", admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void tokenFirmadoConOtraClave_retorna401() throws Exception {
        mockMvc.perform(get("/api/giros").header("Authorization",
                        "Bearer eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJhZG1pbiIsInJvbCI6IkFETUlOIn0.firmaFalsa"))
                .andExpect(status().isUnauthorized());
    }
}
