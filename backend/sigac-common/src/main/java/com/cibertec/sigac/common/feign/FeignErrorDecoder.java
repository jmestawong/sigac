package com.cibertec.sigac.common.feign;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cibertec.sigac.common.exception.BusinessRuleException;
import com.cibertec.sigac.common.exception.DuplicateResourceException;
import com.cibertec.sigac.common.exception.ResourceNotFoundException;

import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * Traduce los errores de otro microservicio a las excepciones de negocio
 * locales, para que el usuario reciba el mismo 404/400/409 con el mensaje
 * original (por ejemplo "Socio no encontrado con id 7").
 */
public class FeignErrorDecoder implements ErrorDecoder {

    private static final Pattern MENSAJE = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

    private final ErrorDecoder porDefecto = new Default();

    @Override
    public Exception decode(String metodo, Response respuesta) {
        String mensaje = extraerMensaje(respuesta);
        return switch (respuesta.status()) {
            case 404 -> new ResourceNotFoundException(mensaje);
            case 400 -> new BusinessRuleException(mensaje);
            case 409 -> new DuplicateResourceException(mensaje);
            default -> porDefecto.decode(metodo, respuesta);
        };
    }

    private String extraerMensaje(Response respuesta) {
        String generico = "Error al consultar otro servicio (HTTP " + respuesta.status() + ")";
        if (respuesta.body() == null) {
            return generico;
        }
        try {
            String cuerpo = new String(respuesta.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = MENSAJE.matcher(cuerpo);
            return matcher.find() ? matcher.group(1) : generico;
        } catch (IOException ex) {
            return generico;
        }
    }
}
