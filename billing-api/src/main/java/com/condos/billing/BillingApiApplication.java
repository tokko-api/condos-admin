package com.condos.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.condos.billing",   // este servicio
        "com.condos.shared"     // JwtService, GlobalExceptionHandler, CorsConfig...
})
@EnableMongoRepositories(basePackages = {
        "com.condos.billing.repository"
})
@EnableScheduling // generación automática de cargos (RN-PAG-01)
public class BillingApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(BillingApiApplication.class, args);
    }
}
