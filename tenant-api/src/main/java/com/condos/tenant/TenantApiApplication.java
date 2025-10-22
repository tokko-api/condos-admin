package com.condos.tenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.condos.tenant",   // este proyecto
        "com.condos.shared"    // para ver JwtService del shared-lib
})
@EnableMongoRepositories(basePackages = {
        "com.condos.tenant"    // ajusta si tienes repos en otro paquete
})
public class TenantApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(TenantApiApplication.class, args);
    }
}