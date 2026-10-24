package com.cibertec.msegresos;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.cibertec.sigac.common.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class EgresosFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private String operador() {
        return "Bearer " + jwtService.generarToken("operador", "OPERADOR");
    }

    private String crearEgreso(String documento) throws Exception {
        String cuerpo = """
                {"numeroDocumento":"%s","proveedor":"Ferreteria Lima","fecha":"2026-10-15","importe":150.50,
                 "documentoAsociado":"F001-100","motivo":"Mantenimiento"}
                """.formatted(documento);
        return mockMvc.perform(post("/api/egresos").header("Authorization", operador())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado", is("REGISTRADO")))
                .andExpect(jsonPath("$.correlativo", startsWith("E-")))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void registrarProcesarYNoPoderAnularUnEgresoProcesado() throws Exception {
        String creado = crearEgreso("D-001");
        Long id = Long.valueOf(creado.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/egresos/" + id).header("Authorization", operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proveedor", is("Ferreteria Lima")));

        mockMvc.perform(post("/api/egresos/" + id + "/procesar").header("Authorization", operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("PROCESADO")));

        mockMvc.perform(post("/api/egresos/" + id + "/anular").header("Authorization", operador()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarPorRangoDeFechas() throws Exception {
        crearEgreso("D-002");

        mockMvc.perform(get("/api/egresos")
                        .param("desde", "2026-10-01").param("hasta", "2026-10-31")
                        .header("Authorization", operador()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/egresos")
                        .param("desde", "2026-11-01").param("hasta", "2026-10-01")
                        .header("Authorization", operador()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cargaMasivaSinArchivo_retorna400() throws Exception {
        mockMvc.perform(post("/api/egresos/carga-masiva").contentType(MediaType.MULTIPART_FORM_DATA)
                        .header("Authorization", operador()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cargaMasivaConArchivoInvalido_retorna400() throws Exception {
        org.springframework.mock.web.MockMultipartFile archivo = new org.springframework.mock.web.MockMultipartFile(
                "archivo", "egresos.xlsx", "application/octet-stream", "no es un excel".getBytes());

        mockMvc.perform(multipart("/api/egresos/carga-masiva").file(archivo).header("Authorization", operador()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void egresoInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/api/egresos/999999").header("Authorization", operador()))
                .andExpect(status().isNotFound());
    }

    @Test
    void egresoConDatosInvalidos_retorna400() throws Exception {
        mockMvc.perform(post("/api/egresos").header("Authorization", operador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"proveedor\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Error de validacion")));
    }
}
