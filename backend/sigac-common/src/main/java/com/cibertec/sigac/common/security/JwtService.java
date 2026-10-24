package com.cibertec.sigac.common.security;

import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Emite (ms-auth) y valida (todos los servicios) los tokens JWT. Todos los
 * microservicios comparten la misma clave de firma mediante la propiedad
 * sigac.security.jwt.secret / variable de entorno JWT_SECRET.
 */
@Service
public class JwtService {

    @Value("${sigac.security.jwt.secret}")
    private String secret;

    @Value("${sigac.security.jwt.expiration-ms:86400000}")
    private long expirationMs;

    public String generarToken(String username, String rol) {
        Date ahora = new Date();
        return Jwts.builder()
                .claims(Map.of("rol", rol))
                .subject(username)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expirationMs))
                .signWith(clave())
                .compact();
    }

    /** Lanza JwtException si el token esta mal formado, vencido o con firma invalida. */
    public Claims leerClaims(String token) {
        return Jwts.parser().verifyWith(clave()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey clave() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
