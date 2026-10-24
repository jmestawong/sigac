package com.cibertec.sigac.common.feign;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/** Reenvia el token del usuario a los servicios que se consumen con OpenFeign. */
public class AuthorizationRelayInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            String autorizacion = atributos.getRequest().getHeader("Authorization");
            if (autorizacion != null) {
                template.header("Authorization", autorizacion);
            }
        }
    }
}
