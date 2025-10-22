package com.condos.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.condos.user",   // este proyecto
        "com.condos.shared"    // para ver JwtService del shared-lib
})
@EnableMongoRepositories(basePackages = {
        "com.condos.user"    // ajusta si tienes repos en otro paquete
})
public class UserApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApiApplication.class, args);
    }
}