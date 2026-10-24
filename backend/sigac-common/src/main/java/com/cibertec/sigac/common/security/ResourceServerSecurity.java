package com.cibertec.sigac.common.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Arma la cadena de seguridad comun de los microservicios (stateless, JWT,
 * respuestas 401/403 en JSON). Cada servicio solo aporta sus reglas por ruta.
 */
@Component
public class ResourceServerSecurity {

    private final JwtAuthenticationFilter jwtFilter;

    public ResourceServerSecurity(JwtService jwtService) {
        this.jwtFilter = new JwtAuthenticationFilter(jwtService);
    }

    public SecurityFilterChain construir(HttpSecurity http,
            Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> reglas)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas)
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((request, response, ex) -> escribirError(response,
                                HttpStatus.UNAUTHORIZED, "Debe iniciar sesion para acceder a este recurso"))
                        .accessDeniedHandler((request, response, ex) -> escribirError(response,
                                HttpStatus.FORBIDDEN, "No tiene permisos para acceder a este recurso")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Escribe el JSON directamente (sin response.sendError): sendError() hace un
     * forward interno a /error que vuelve a pasar por la cadena y pisaba el 403
     * original con un 401.
     */
    private void escribirError(HttpServletResponse response, HttpStatus status, String mensaje) throws IOException {
        String json = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","detalles":null}"""
                .formatted(LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(json);
    }
}
