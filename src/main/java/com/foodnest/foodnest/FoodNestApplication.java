package com.foodnest.foodnest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class FoodNestApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodNestApplication.class, args);
    }
}
