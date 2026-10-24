package com.cibertec.sigac.common.auditoria;

/** Nombres de la topologia RabbitMQ de la auditoria (productor y consumidor deben coincidir). */
public final class AuditoriaAmqp {

    public static final String EXCHANGE = "sigac.auditoria.exchange";
    public static final String QUEUE = "sigac.auditoria.queue";
    public static final String ROUTING_KEY = "auditoria.evento";

    private AuditoriaAmqp() {
    }
}
