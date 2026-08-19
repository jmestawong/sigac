package com.cibertec.sigac.config;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.cibertec.sigac.security.CustomUserDetailsService;
import com.cibertec.sigac.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Catalogos que solo el administrador puede crear/editar/eliminar (RF del
     * actor Administrador: "gestiona catalogos de socios, puestos, giros,
     * servicios y bancos"). La consulta (GET) queda abierta a cualquier
     * usuario autenticado, ya que el Operador de caja necesita buscarlos
     * para cobranza.
     */
    private static final String[] CATALOGOS_ADMIN = {
            "/api/socios", "/api/socios/**",
            "/api/puestos", "/api/puestos/**",
            "/api/giros", "/api/giros/**",
            "/api/servicios", "/api/servicios/**",
            "/api/bancos", "/api/bancos/**",
    };

    /**
     * La generacion masiva y la eliminacion de cuentas por cobrar quedan
     * reservadas al administrador; la consulta (listado, detalle y resumen
     * por socio/puesto) sigue abierta a cualquier autenticado, ya que el
     * Operador de caja la usa para consultar deudas.
     */
    private static final String[] GENERACION_CUENTAS_ADMIN = {
            "/api/cuentas-por-cobrar/generar/**",
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, CATALOGOS_ADMIN).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, CATALOGOS_ADMIN).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, CATALOGOS_ADMIN).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, GENERACION_CUENTAS_ADMIN).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/cuentas-por-cobrar/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Escribe la respuesta JSON directamente (sin response.sendError), porque
     * sendError() dispara un forward interno a "/error" que vuelve a pasar por
     * la cadena de filtros; como JwtAuthenticationFilter no se ejecuta en ese
     * dispatch (OncePerRequestFilter lo omite por defecto), ese segundo paso
     * llega sin autenticacion y el 403 original terminaba pisado por un 401.
     */
    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> writeError(response, HttpStatus.FORBIDDEN,
                "No tiene permisos para acceder a este recurso");
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> writeError(response, HttpStatus.UNAUTHORIZED,
                "Debe iniciar sesion para acceder a este recurso");
    }

    private void writeError(jakarta.servlet.http.HttpServletResponse response, HttpStatus status, String message)
            throws java.io.IOException {
        String json = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","detalles":null}"""
                .formatted(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(json);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
