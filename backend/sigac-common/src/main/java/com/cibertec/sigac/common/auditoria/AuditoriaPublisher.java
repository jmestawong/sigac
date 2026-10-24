package com.cibertec.sigac.common.auditoria;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Publica los eventos de auditoria de forma asincrona. Es "mejor esfuerzo":
 * si RabbitMQ no esta disponible, la operacion de negocio (por ejemplo, el
 * cobro) NO falla; solo se deja una advertencia en el log.
 */
public class AuditoriaPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final boolean habilitada;

    public AuditoriaPublisher(RabbitTemplate rabbitTemplate, boolean habilitada) {
        this.rabbitTemplate = rabbitTemplate;
        this.habilitada = habilitada;
    }

    public void registrar(String modulo, String accion, String referencia, String detalle, BigDecimal monto) {
        if (!habilitada || rabbitTemplate == null) {
            return;
        }
        AuditoriaEvento evento = AuditoriaEvento.builder()
                .modulo(modulo)
                .accion(accion)
                .referencia(referencia)
                .detalle(detalle)
                .monto(monto)
                .usuario(usuarioActual())
                .fecha(LocalDateTime.now())
                .build();
        try {
            rabbitTemplate.convertAndSend(AuditoriaAmqp.EXCHANGE, AuditoriaAmqp.ROUTING_KEY, evento);
        } catch (RuntimeException ex) {
            log.warn("No se pudo publicar el evento de auditoria {} {}: {}", accion, referencia, ex.getMessage());
        }
    }

    private String usuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null ? autenticacion.getName() : "sistema";
    }
}
