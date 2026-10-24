package com.cibertec.sigac.common.feign;

import java.time.LocalDateTime;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cibertec.sigac.common.dto.ErrorResponse;

import feign.FeignException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FeignExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handle(FeignException ex) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("Un servicio requerido no esta disponible en este momento. Intente nuevamente.")
                .build();
        return ResponseEntity.status(status).body(cuerpo);
    }
}
