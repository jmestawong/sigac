package com.cibertec.msauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.cibertec.msauth.entidades.Usuario;
import com.cibertec.msauth.repositorio.UsuarioRepository;
import com.cibertec.sigac.common.security.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String loginJson(String username, String password) {
        return "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
    }

    @Test
    void login_conCredencialesValidas_retornaTokenJwtUsernameYRol() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.tipo", is("Bearer")))
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.rol", is("ADMIN")));
    }

    @Test
    void login_conRolOperador_retornaElRolCorrespondiente() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("operador", "operador123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("OPERADOR")));
    }

    @Test
    void login_conPasswordIncorrecto_retorna401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("admin", "clave-equivocada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void login_conUsuarioInexistente_retorna401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("fantasma", "loquesea")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_conCamposEnBlanco_retorna400ConElDetalleDeLasValidaciones() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Error de validacion")))
                .andExpect(jsonPath("$.detalles", hasItem("username: El username es obligatorio")))
                .andExpect(jsonPath("$.detalles", hasItem("password: El password es obligatorio")));
    }

    @Test
    void login_sinCuerpo_retorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_conMetodoGet_retorna405() throws Exception {
        mockMvc.perform(get("/api/auth/login")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void tokenEmitido_llevaElUsuarioYElRolYEsValidoParaLosDemasServicios() throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("operador", "operador123")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = respuesta.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        Claims claims = jwtService.leerClaims(token);

        assertThat(claims.getSubject()).isEqualTo("operador");
        assertThat(claims.get("rol", String.class)).isEqualTo("OPERADOR");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void tokenInvalido_esRechazadoPorElValidador() {
        assertThatThrownBy(() -> jwtService.leerClaims("token.invalido.xyz")).isInstanceOf(JwtException.class);
    }

    @Test
    void passwordsSembrados_estanCifradosConBCryptEnLaBaseDeDatos() {
        Usuario admin = usuarioRepository.findByUsername("admin").orElseThrow();
        Usuario operador = usuarioRepository.findByUsername("operador").orElseThrow();

        assertThat(admin.getPassword()).isNotEqualTo("admin123").startsWith("$2");
        assertThat(operador.getPassword()).isNotEqualTo("operador123").startsWith("$2");
        assertThat(passwordEncoder.matches("admin123", admin.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("operador123", operador.getPassword())).isTrue();
    }
}
