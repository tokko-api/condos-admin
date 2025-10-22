package com.condos.board;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.condos.board",   // este servicio
        "com.condos.shared"   // donde tengas JwtService u otras libs
})
@EnableMongoRepositories(basePackages = {
        "com.condos.board.repository"  // ajusta a tus repos Mongo
})
public class BoardApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(BoardApiApplication.class, args);
    }
}