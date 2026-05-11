package com.gpiv.atlanticsprinttech;

import org.n52.jackson.datatype.jts.JtsModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // Añadido para habilitar la ejecución asíncrona
public class AplicacionGestionGpiv {
    public static void main(String[] args) {

        SpringApplication.run(AplicacionGestionGpiv.class, args);
    }
    // Registra el módulo para que Spring Boot envíe GeoJSON al frontend
    @Bean
    public JtsModule jtsModule() {
        return new JtsModule();
    }
}
