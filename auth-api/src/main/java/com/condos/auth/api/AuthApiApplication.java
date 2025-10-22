package com.condos.auth.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.condos.auth",      // controllers, services, modelos locales
        "com.condos.shared"     // si usas clases del shared-lib
})
@EnableMongoRepositories(basePackages = {
        "com.condos.auth.*" // paquete donde está el UserRepository
})
public class AuthApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApiApplication.class, args);
    }
}