package com.cibertec.mscobranza;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsCobranzaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsCobranzaApplication.class, args);
    }
}
