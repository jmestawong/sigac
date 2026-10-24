package com.cibertec.msauditoria.negocio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.cibertec.sigac.common.auditoria.AuditoriaAmqp;
import com.cibertec.sigac.common.auditoria.AuditoriaEvento;

import lombok.RequiredArgsConstructor;

/** Consume los eventos que publican ms-cobranza y ms-egresos en la cola de RabbitMQ y los guarda en la bitacora. */
@Component
@RequiredArgsConstructor
public class AuditoriaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaConsumer.class);

    private final AuditoriaService auditoriaService;

    @RabbitListener(queues = AuditoriaAmqp.QUEUE)
    public void consumir(AuditoriaEvento evento) {
        auditoriaService.registrar(evento);
        log.info("Auditoria registrada: {} {} por {}", evento.getAccion(), evento.getReferencia(), evento.getUsuario());
    }
}
