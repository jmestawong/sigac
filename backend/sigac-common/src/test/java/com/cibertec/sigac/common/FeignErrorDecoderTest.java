package com.cibertec.sigac.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.cibertec.sigac.common.exception.BusinessRuleException;
import com.cibertec.sigac.common.exception.DuplicateResourceException;
import com.cibertec.sigac.common.exception.ResourceNotFoundException;
import com.cibertec.sigac.common.feign.FeignErrorDecoder;

import feign.FeignException;
import feign.Request;
import feign.Response;

class FeignErrorDecoderTest {

    private final FeignErrorDecoder decoder = new FeignErrorDecoder();

    private Response respuesta(int status, String cuerpo) {
        Map<String, Collection<String>> cabeceras = Map.of();
        Request peticion = Request.create(Request.HttpMethod.GET, "http://ms-catalogos/api/socios/7", cabeceras, null,
                StandardCharsets.UTF_8, null);
        return Response.builder().status(status).reason("x").request(peticion).headers(cabeceras)
                .body(cuerpo, StandardCharsets.UTF_8).build();
    }

    @Test
    void un404SeConvierteEnRecursoNoEncontradoConElMensajeOriginal() {
        Exception ex = decoder.decode("obtenerSocio",
                respuesta(404, "{\"status\":404,\"message\":\"Socio no encontrado con id 7\",\"detalles\":null}"));

        assertThat(ex).isInstanceOf(ResourceNotFoundException.class).hasMessage("Socio no encontrado con id 7");
    }

    @Test
    void un400SeConvierteEnReglaDeNegocio() {
        Exception ex = decoder.decode("x", respuesta(400, "{\"message\":\"Dato invalido\"}"));

        assertThat(ex).isInstanceOf(BusinessRuleException.class).hasMessage("Dato invalido");
    }

    @Test
    void un409SeConvierteEnRecursoDuplicado() {
        Exception ex = decoder.decode("x", respuesta(409, "{\"message\":\"Ya existe\"}"));

        assertThat(ex).isInstanceOf(DuplicateResourceException.class).hasMessage("Ya existe");
    }

    @Test
    void sinMensajeEnElCuerpoUsaUnTextoGenerico() {
        Exception ex = decoder.decode("x", respuesta(404, "<html>no es json</html>"));

        assertThat(ex).isInstanceOf(ResourceNotFoundException.class);
        assertThat(ex.getMessage()).contains("HTTP 404");
    }

    @Test
    void otrosCodigosQuedanComoFeignException() {
        Exception ex = decoder.decode("x", respuesta(500, "{\"message\":\"boom\"}"));

        assertThat(ex).isInstanceOf(FeignException.class);
    }
}
