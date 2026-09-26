package com.danza_check.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros de emision y validacion de tokens JWT.
 * Se inyectan desde JWT_SECRET, JWT_ISSUER y JWT_EXPIRATION_MINUTES.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, String issuer, long expirationMinutes) {
}
