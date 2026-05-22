package ru.mtuci.coursemanagement.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import io.jsonwebtoken.security.Keys;

@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(
        String issuer,
        Duration accessTtl,
        Duration refreshTtl,
        @NotBlank String secret
) {
    public JwtProperties {
        issuer = issuer == null ? "practica" : issuer;
        accessTtl = accessTtl == null ? Duration.ofMinutes(15) : accessTtl;
        refreshTtl = refreshTtl == null ? Duration.ofDays(7) : refreshTtl;
    }

    /** HS256 ключ; секрет должен быть не короче 256 бит. */
    public SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
