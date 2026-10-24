package com.cibertec.sigac.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import com.cibertec.sigac.common.feign.AuthorizationRelayInterceptor;
import com.cibertec.sigac.common.feign.FeignErrorDecoder;
import com.cibertec.sigac.common.feign.FeignExceptionHandler;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;

/** Solo se activa en los servicios que usan OpenFeign. */
@AutoConfiguration
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class FeignCommonAutoConfiguration {

    @Bean
    public RequestInterceptor authorizationRelayInterceptor() {
        return new AuthorizationRelayInterceptor();
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new FeignErrorDecoder();
    }

    @Bean
    public FeignExceptionHandler feignExceptionHandler() {
        return new FeignExceptionHandler();
    }
}
