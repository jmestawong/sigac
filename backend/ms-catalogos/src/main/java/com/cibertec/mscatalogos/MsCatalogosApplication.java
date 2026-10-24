package com.cibertec.mscatalogos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsCatalogosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsCatalogosApplication.class, args);
    }
}
