package com.kaveri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class KaveriApplication {

    public static void main(String[] args) {
        SpringApplication.run(KaveriApplication.class, args);
    }
}
