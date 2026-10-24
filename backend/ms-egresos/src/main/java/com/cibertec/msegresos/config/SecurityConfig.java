package com.cibertec.msegresos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    /** Los egresos los opera tanto el ADMIN como el OPERADOR de caja: basta con estar autenticado. */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return seguridad.construir(http, auth -> auth.anyRequest().authenticated());
    }
}
