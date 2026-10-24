package com.cibertec.sigac.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cibertec.sigac.common.security.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final String CLAVE = "EZ8HgNtU/HNryDDLFKmXbm9JfL9K027pzTXOFwMY47sia4+Y0HQIn4EidDuGWWDS";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = servicio(CLAVE, 60_000);
    }

    private JwtService servicio(String clave, long expiracionMs) {
        JwtService servicio = new JwtService();
        ReflectionTestUtils.setField(servicio, "secret", clave);
        ReflectionTestUtils.setField(servicio, "expirationMs", expiracionMs);
        return servicio;
    }

    @Test
    void elTokenEmitidoLlevaUsuarioYRol() {
        Claims claims = jwtService.leerClaims(jwtService.generarToken("operador", "OPERADOR"));

        assertThat(claims.getSubject()).isEqualTo("operador");
        assertThat(claims.get("rol", String.class)).isEqualTo("OPERADOR");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void unTokenFirmadoConOtraClaveEsRechazado() {
        String ajeno = servicio("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 60_000)
                .generarToken("admin", "ADMIN");

        assertThatThrownBy(() -> jwtService.leerClaims(ajeno)).isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenVencidoEsRechazado() {
        String vencido = servicio(CLAVE, -1_000).generarToken("admin", "ADMIN");

        assertThatThrownBy(() -> jwtService.leerClaims(vencido)).isInstanceOf(JwtException.class);
    }

    @Test
    void unTextoCualquieraNoEsUnToken() {
        assertThatThrownBy(() -> jwtService.leerClaims("esto-no-es-un-jwt")).isInstanceOf(JwtException.class);
    }
}
