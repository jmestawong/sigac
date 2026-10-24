package com.cibertec.mscobranza.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.cibertec.sigac.common.security.ResourceServerSecurity;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ResourceServerSecurity seguridad;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return seguridad.construir(http, auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/cuentas-por-cobrar/generar/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/cuentas-por-cobrar/**").hasRole("ADMIN")
                .anyRequest().authenticated());
    }
}
