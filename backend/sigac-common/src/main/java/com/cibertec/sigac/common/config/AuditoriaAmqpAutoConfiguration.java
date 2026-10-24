package com.cibertec.sigac.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import com.cibertec.sigac.common.auditoria.AuditoriaAmqp;
import com.cibertec.sigac.common.auditoria.AuditoriaPublisher;

@AutoConfiguration(after = RabbitAutoConfiguration.class)
@ConditionalOnClass(RabbitTemplate.class)
public class AuditoriaAmqpAutoConfiguration {

    @Bean
    public DirectExchange auditoriaExchange() {
        return new DirectExchange(AuditoriaAmqp.EXCHANGE, true, false);
    }

    @Bean
    public Queue auditoriaQueue() {
        return new Queue(AuditoriaAmqp.QUEUE, true);
    }

    @Bean
    public Binding auditoriaBinding(Queue auditoriaQueue, DirectExchange auditoriaExchange) {
        return BindingBuilder.bind(auditoriaQueue).to(auditoriaExchange).with(AuditoriaAmqp.ROUTING_KEY);
    }

    @Bean
    public MessageConverter auditoriaMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public AuditoriaPublisher auditoriaPublisher(ObjectProvider<RabbitTemplate> rabbitTemplate,
            @Value("${sigac.auditoria.habilitada:true}") boolean habilitada) {
        return new AuditoriaPublisher(rabbitTemplate.getIfAvailable(), habilitada);
    }
}
