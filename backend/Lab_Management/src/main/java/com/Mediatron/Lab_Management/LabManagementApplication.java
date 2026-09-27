package com.Mediatron.Lab_Management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableAsync
public class LabManagementApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(LabManagementApplication.class);
        app.setDefaultProperties(defaultProperties());
        app.run(args);
    }

    private static Map<String, Object> defaultProperties() {
        Map<String, Object> defaults = new HashMap<>();
        ParsedDatabaseUrl parsedDatabaseUrl = parseDatabaseUrl(System.getenv("DATABASE_URL"));

        String datasourceUrl = getenvOrNull("SPRING_DATASOURCE_URL");
        if (!hasText(datasourceUrl)) {
            datasourceUrl = parsedDatabaseUrl.jdbcUrl != null
                    ? parsedDatabaseUrl.jdbcUrl
                    : "jdbc:postgresql://localhost:5432/mediatron";
        }
        datasourceUrl = sanitizeJdbcUrl(datasourceUrl);

        String datasourceUsername = getenvOrNull("SPRING_DATASOURCE_USERNAME");
        if (!hasText(datasourceUsername)) {
            datasourceUsername = parsedDatabaseUrl.username != null ? parsedDatabaseUrl.username : "postgres";
        }

        String datasourcePassword = getenvOrNull("SPRING_DATASOURCE_PASSWORD");
        if (!hasText(datasourcePassword)) {
            datasourcePassword = parsedDatabaseUrl.password != null ? parsedDatabaseUrl.password : "postgres";
        }

        defaults.put("spring.datasource.url", datasourceUrl);
        defaults.put("spring.datasource.username", datasourceUsername);
        defaults.put("spring.datasource.password", datasourcePassword);
        defaults.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

        defaults.put("spring.jpa.hibernate.ddl-auto",
                System.getenv().getOrDefault("SPRING_JPA_HIBERNATE_DDL_AUTO", "none"));
        defaults.put("spring.jpa.open-in-view", "false");
        defaults.put("spring.flyway.enabled",
                System.getenv().getOrDefault("SPRING_FLYWAY_ENABLED", "true"));
        defaults.put("spring.flyway.locations", "classpath:db/migration");

        defaults.put("mediatron.app.jwtSecret",
                System.getenv().getOrDefault("MEDIATRON_APP_JWT_SECRET",
                        "U29tZVZlcnlMb25nU2VjcmV0S2V5Rm9ySFM1NlRva2VuU2lnbmluZw=="));
        defaults.put("mediatron.app.jwtExpirationMs",
                System.getenv().getOrDefault("MEDIATRON_APP_JWT_EXPIRATION_MS", "86400000"));

        defaults.put("server.port", System.getenv().getOrDefault("SERVER_PORT", "8080"));
        return defaults;
    }

    private static ParsedDatabaseUrl parseDatabaseUrl(String rawDatabaseUrl) {
        if (!hasText(rawDatabaseUrl)) {
            return ParsedDatabaseUrl.empty();
        }

        try {
            URI uri = URI.create(rawDatabaseUrl.trim());
            String scheme = uri.getScheme();
            if (!"postgresql".equalsIgnoreCase(scheme) && !"postgres".equalsIgnoreCase(scheme)) {
                return ParsedDatabaseUrl.empty();
            }

            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String dbName = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
            if (!hasText(host) || !hasText(dbName)) {
                return ParsedDatabaseUrl.empty();
            }

            String query = uri.getRawQuery();
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName +
                    (hasText(query) ? "?" + query : "");

            String username = null;
            String password = null;
            String userInfo = uri.getUserInfo();
            if (hasText(userInfo)) {
                String[] parts = userInfo.split(":", 2);
                username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                if (parts.length == 2) {
                    password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                }
            }

            return new ParsedDatabaseUrl(jdbcUrl, username, password);
        } catch (Exception ignored) {
            return ParsedDatabaseUrl.empty();
        }
    }

    private static String getenvOrNull(String key) {
        String value = System.getenv(key);
        return hasText(value) ? value : null;
    }

    private static String sanitizeJdbcUrl(String jdbcUrl) {
        if (!hasText(jdbcUrl) || !jdbcUrl.contains("channel_binding=require")) {
            return jdbcUrl;
        }

        String sanitized = jdbcUrl
                .replace("&channel_binding=require", "")
                .replace("?channel_binding=require&", "?")
                .replace("?channel_binding=require", "")
                .replace("&&", "&");

        if (sanitized.endsWith("?") || sanitized.endsWith("&")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }

        return sanitized;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record ParsedDatabaseUrl(String jdbcUrl, String username, String password) {
        private static ParsedDatabaseUrl empty() {
            return new ParsedDatabaseUrl(null, null, null);
        }
    }
}
