package com.cibertec.mscobranza;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.mscobranza.client.CatalogosClient;
import com.cibertec.sigac.common.security.JwtService;

/**
 * Flujo de cobranza de punta a punta contra H2, con ms-catalogos simulado:
 * generar cuentas (ADMIN), cobrarlas (OPERADOR), emitir el recibo con su
 * correlativo y consultar los recibos. Verifica tambien las reglas de rol.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CobranzaFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CatalogosClient catalogosClient;

    @BeforeEach
    void catalogos() {
        CobranzaTestSupport.configurarCatalogos(catalogosClient);
    }

    private String admin() {
        return "Bearer " + jwtService.generarToken("admin", "ADMIN");
    }

    private String operador() {
        return "Bearer " + jwtService.generarToken("operador", "OPERADOR");
    }

    private Long generarCuentaDePuesto() throws Exception {
        String cuerpo = """
                {"servicioId":1,"periodo":"2026-10","monto":50.00,"puestoIds":[1]}
                """;
        String respuesta = mockMvc.perform(post("/api/cuentas-por-cobrar/generar/puestos-monto-fijo")
                        .header("Authorization", admin()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estado", is("PENDIENTE")))
                .andExpect(jsonPath("$[0].puesto.numero", is("P-01")))
                .andExpect(jsonPath("$[0].servicio.nombre", is("Mantenimiento")))
                .andReturn().getResponse().getContentAsString();
        return Long.valueOf(respuesta.replaceAll("^\\[\\{\"id\":(\\d+).*", "$1"));
    }

    @Test
    void generarCobrarYEmitirElRecibo() throws Exception {
        Long cuentaId = generarCuentaDePuesto();

        mockMvc.perform(get("/api/cuentas-por-cobrar/" + cuentaId).header("Authorization", operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.socio").doesNotExist())
                .andExpect(jsonPath("$.puesto.socio.codigo", is("S-001")));

        String pago = """
                {"cuentasAbonadasIds":[%d],"cuentasExoneradasIds":[]}
                """.formatted(cuentaId);
        String respuesta = mockMvc.perform(post("/api/pagos/procesar-cuentas")
                        .header("Authorization", operador()).contentType(MediaType.APPLICATION_JSON).content(pago))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recibo.correlativo", startsWith("R-0000")))
                .andExpect(jsonPath("$.recibo.tipo", is("PAGO_CUENTAS")))
                .andExpect(jsonPath("$.recibo.monto", is(50.00)))
                .andExpect(jsonPath("$.cuentasActualizadas[0].estado", is("ABONADA")))
                .andReturn().getResponse().getContentAsString();
        Long reciboId = Long.valueOf(respuesta.replaceAll(".*\"recibo\":\\{\"id\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/recibos/" + reciboId).header("Authorization", operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuentas", hasSize(1)))
                .andExpect(jsonPath("$.cuentas[0].reciboCorrelativo", startsWith("R-0000")));

        mockMvc.perform(get("/api/recibos").param("tipo", "PAGO_CUENTAS").header("Authorization", operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // La cuenta ya fue procesada: no se puede cobrar dos veces
        mockMvc.perform(post("/api/pagos/procesar-cuentas")
                        .header("Authorization", operador()).contentType(MediaType.APPLICATION_JSON).content(pago))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingresoExternoYCanjeBancario() throws Exception {
        String ingreso = """
                {"depositante":"Municipalidad","categoria":"Donacion","concepto":"Aporte","monto":500.00}
                """;
        mockMvc.perform(post("/api/pagos/ingreso-externo")
                        .header("Authorization", operador()).contentType(MediaType.APPLICATION_JSON).content(ingreso))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo", is("INGRESO_EXTERNO")));

        String generar = """
                {"servicioId":2,"periodo":"2026","monto":100.00,"sociosUnicos":false}
                """;
        String generadas = mockMvc.perform(post("/api/cuentas-por-cobrar/generar/socios")
                        .header("Authorization", admin()).contentType(MediaType.APPLICATION_JSON).content(generar))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].socio.codigo", is("S-001")))
                .andReturn().getResponse().getContentAsString();
        Long cuentaId = Long.valueOf(generadas.replaceAll("^\\[\\{\"id\":(\\d+).*", "$1"));

        String canje = """
                {"cuentaId":%d,"bancoId":1,"fechaDeposito":"2026-10-18"}
                """.formatted(cuentaId);
        mockMvc.perform(post("/api/pagos/canje-bancario")
                        .header("Authorization", operador()).contentType(MediaType.APPLICATION_JSON).content(canje))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo", is("CANJE_BANCARIO")))
                .andExpect(jsonPath("$.banco.nombre", is("BCP")));
    }

    @Test
    void elOperadorNoPuedeGenerarNiEliminarCuentas() throws Exception {
        String cuerpo = """
                {"servicioId":1,"periodo":"2026-10","monto":50.00,"puestoIds":[1]}
                """;
        mockMvc.perform(post("/api/cuentas-por-cobrar/generar/puestos-monto-fijo")
                        .header("Authorization", operador()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/cuentas-por-cobrar/1").header("Authorization", operador()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/cuentas-por-cobrar").header("Authorization", operador()))
                .andExpect(status().isOk());
    }

    @Test
    void referenciasInformaCuantasCuentasUsanUnSocio() throws Exception {
        generarCuentaDePuesto();

        mockMvc.perform(get("/api/cuentas-por-cobrar/referencias/puesto/1").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad", is(1)));
        mockMvc.perform(get("/api/cuentas-por-cobrar/referencias/socio/999").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad", is(0)));
        mockMvc.perform(get("/api/cuentas-por-cobrar/referencias/marciano/1").header("Authorization", admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resumenPorSocioYPorPuesto() throws Exception {
        generarCuentaDePuesto();

        mockMvc.perform(get("/api/cuentas-por-cobrar/resumen/socio/1").header("Authorization", operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.socio.codigo", is("S-001")))
                .andExpect(jsonPath("$.cuentasPuestos", hasSize(1)));
        mockMvc.perform(get("/api/cuentas-por-cobrar/resumen/puesto/1").header("Authorization", operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puesto.numero", is("P-01")))
                .andExpect(jsonPath("$.cuentasPuesto", hasSize(1)));
    }

    @Test
    void siMsCatalogosNoResponde_elClienteRecibe503() throws Exception {
        org.mockito.Mockito.when(catalogosClient.obtenerServicio(1L)).thenThrow(
                feign.FeignException.errorStatus("obtenerServicio", feign.Response.builder()
                        .status(500).reason("boom").request(feign.Request.create(feign.Request.HttpMethod.GET,
                                "http://ms-catalogos/api/servicios/1", java.util.Map.of(), null, null, null))
                        .headers(java.util.Map.of()).build()));

        String cuerpo = """
                {"servicioId":1,"periodo":"2026-10","monto":50.00,"puestoIds":[1]}
                """;
        mockMvc.perform(post("/api/cuentas-por-cobrar/generar/puestos-monto-fijo")
                        .header("Authorization", admin()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", is(503)));
    }
}
