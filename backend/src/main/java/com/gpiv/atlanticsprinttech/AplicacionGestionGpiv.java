package com.gpiv.atlanticsprinttech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AplicacionGestionGpiv {
    public static void main(String[] args) {
        SpringApplication.run(AplicacionGestionGpiv.class, args);
    }
}
