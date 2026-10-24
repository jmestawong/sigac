package com.cibertec.msreportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

import com.cibertec.msreportes.client.CobranzaClient;
import com.cibertec.msreportes.client.EgresosClient;
import com.cibertec.sigac.common.security.JwtService;

/** Descargas XLSX de ms-reportes (con ms-cobranza y ms-egresos simulados) y su seguridad. */
@SpringBootTest
@AutoConfigureMockMvc
class ReportesIntegrationTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CobranzaClient cobranzaClient;

    @MockitoBean
    private EgresosClient egresosClient;

    @BeforeEach
    void clientesVacios() {
        when(cobranzaClient.listarRecibos()).thenReturn(List.of());
        when(egresosClient.listarPorRango(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))).thenReturn(List.of());
    }

    private String bearer(String rol) {
        return "Bearer " + jwtService.generarToken(rol.toLowerCase(), rol);
    }

    private List<String> rutas() {
        List<String> rutas = new ArrayList<>();
        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
            if (handler.getBeanType().getPackageName().equals("com.cibertec.msreportes.rest")) {
                Set<String> patrones = info.getPathPatternsCondition().getPatternValues();
                rutas.addAll(patrones);
            }
        });
        return rutas;
    }

    @Test
    void existenLosSeisReportes() {
        assertThat(rutas()).containsExactlyInAnyOrder("/api/reportes/movimientos", "/api/reportes/totales",
                "/api/reportes/socios", "/api/reportes/no-socios", "/api/reportes/egresos", "/api/reportes/bancos");
    }

    @Test
    void cadaReporteSeDescargaComoXlsxParaAdminYOperador() throws Exception {
        for (String rol : List.of("ADMIN", "OPERADOR")) {
            for (String ruta : rutas()) {
                mockMvc.perform(get(ruta).param("desde", "2026-08-01").param("hasta", "2026-08-31")
                                .header("Authorization", bearer(rol)))
                        .andExpect(status().isOk())
                        .andExpect(header().string("Content-Type", XLSX))
                        .andExpect(header().string("Content-Disposition", containsString(".xlsx")));
            }
        }
    }

    @Test
    void sinTokenOConTokenInvalido_retorna401() throws Exception {
        for (String ruta : rutas()) {
            mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
            mockMvc.perform(request(HttpMethod.GET, ruta).header("Authorization", "Bearer x.y.z"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void sinParametrosObligatorios_retorna400IndicandoCualFalta() throws Exception {
        mockMvc.perform(get("/api/reportes/totales").header("Authorization", bearer("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("desde")));
    }

    @Test
    void conFechaInvalida_retorna400() throws Exception {
        mockMvc.perform(get("/api/reportes/totales").param("desde", "no-es-fecha").param("hasta", "2026-12-31")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("desde")));
    }

    @Test
    void conRangoInvertido_retorna400() throws Exception {
        mockMvc.perform(get("/api/reportes/egresos").param("desde", "2026-09-01").param("hasta", "2026-08-01")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isBadRequest());
    }
}
