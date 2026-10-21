package com.kaveri.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes cloud provider database environment variables (Render, Railway, Heroku)
 * into standard Spring Boot JDBC properties before DataSource auto-configuration runs.
 *
 * Converts 'postgres://' or 'postgresql://' URIs into 'jdbc:postgresql://...'
 * and handles individual DB_HOST, DB_PORT, DB_NAME variables seamlessly.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbHost = environment.getProperty("DB_HOST");
        String databaseUrl = environment.getProperty("DATABASE_URL");

        Map<String, Object> props = new HashMap<>();

        // 1. If DB_HOST is explicitly provided (Render individual properties)
        if (dbHost != null && !dbHost.isBlank()) {
            String dbPort = environment.getProperty("DB_PORT", "5432");
            String dbName = environment.getProperty("DB_NAME", "kaveridb");
            String dbUser = environment.getProperty("DB_USER", "");
            String dbPass = environment.getProperty("DB_PASSWORD", "");

            String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);
            props.put("spring.datasource.url", jdbcUrl);
            if (!dbUser.isBlank()) {
                props.put("spring.datasource.username", dbUser);
            }
            if (!dbPass.isBlank()) {
                props.put("spring.datasource.password", dbPass);
            }
            props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        }
        // 2. If DATABASE_URL is provided (e.g. postgres:// or postgresql://)
        else if (databaseUrl != null && !databaseUrl.isBlank()) {
            if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
                try {
                    URI uri = new URI(databaseUrl);
                    String host = uri.getHost();
                    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    String path = uri.getPath(); // includes leading '/'

                    String jdbcUrl = String.format("jdbc:postgresql://%s:%d%s", host, port, path);
                    props.put("spring.datasource.url", jdbcUrl);

                    if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
                        String[] parts = uri.getUserInfo().split(":", 2);
                        props.put("spring.datasource.username", parts[0]);
                        if (parts.length > 1) {
                            props.put("spring.datasource.password", parts[1]);
                        }
                    }
                    props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
                } catch (Exception e) {
                    System.err.println("[DatabaseEnvironmentPostProcessor] Warning: Could not parse DATABASE_URL URI: " + e.getMessage());
                }
            } else if (databaseUrl.startsWith("jdbc:postgresql:")) {
                props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            } else if (databaseUrl.startsWith("jdbc:mysql:")) {
                props.put("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
            }
        }

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("cloudDatabaseProperties", props));
        }
    }
}
