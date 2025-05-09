package com.posementor.posementorbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.posementor.posementorbackend")
public class PosementorBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(PosementorBackendApplication.class, args);
    }
}

