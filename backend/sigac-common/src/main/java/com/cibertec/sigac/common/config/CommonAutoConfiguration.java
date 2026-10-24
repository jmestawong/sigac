package com.cibertec.sigac.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/** Registra el manejador global de errores y la seguridad JWT en cada microservicio. */
@AutoConfiguration
@ComponentScan(basePackages = { "com.cibertec.sigac.common.exception", "com.cibertec.sigac.common.security" })
public class CommonAutoConfiguration {
}
