package com.cibertec.msauditoria;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.msauditoria.negocio.AuditoriaConsumer;
import com.cibertec.sigac.common.auditoria.AuditoriaEvento;
import com.cibertec.sigac.common.security.JwtService;

/**
 * El consumidor (que en produccion recibe el mensaje de RabbitMQ) guarda el
 * evento en la bitacora y solo el ADMIN puede consultarla por la API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditoriaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditoriaConsumer consumidor;

    @Autowired
    private JwtService jwtService;

    private String bearer(String rol) {
        return "Bearer " + jwtService.generarToken(rol.toLowerCase(), rol);
    }

    @Test
    void unEventoConsumidoApareceEnLaBitacoraDelAdmin() throws Exception {
        consumidor.consumir(AuditoriaEvento.builder().modulo("COBRANZA").accion("RECIBO_EMITIDO")
                .referencia("R-000001").detalle("1 cuenta abonada").monto(new BigDecimal("50.00"))
                .usuario("operador").fecha(LocalDateTime.of(2026, 10, 20, 9, 0)).build());

        mockMvc.perform(get("/api/auditoria").header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].usuario", is("operador")))
                .andExpect(jsonPath("$[0].accion", is("RECIBO_EMITIDO")))
                .andExpect(jsonPath("$[0].referencia", is("R-000001")));
    }

    @Test
    void laBitacoraSeFiltraPorModulo() throws Exception {
        consumidor.consumir(AuditoriaEvento.builder().modulo("COBRANZA").accion("RECIBO_EMITIDO").usuario("operador").build());
        consumidor.consumir(AuditoriaEvento.builder().modulo("EGRESOS").accion("EGRESO_REGISTRADO").usuario("admin").build());

        mockMvc.perform(get("/api/auditoria").param("modulo", "EGRESOS").header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accion", is("EGRESO_REGISTRADO")));
    }

    @Test
    void elOperadorNoPuedeConsultarLaBitacora() throws Exception {
        mockMvc.perform(get("/api/auditoria").header("Authorization", bearer("OPERADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenOConTokenInvalido_retorna401() throws Exception {
        mockMvc.perform(get("/api/auditoria")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auditoria").header("Authorization", "Bearer x.y.z"))
                .andExpect(status().isUnauthorized());
    }
}
