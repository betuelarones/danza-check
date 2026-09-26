package com.danza_check.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciales del unico administrador (delegado de danza).
 * Se inyectan desde ADMIN_USERNAME y ADMIN_PASSWORD.
 */
@ConfigurationProperties(prefix = "app.security.admin")
public record AdminProperties(String username, String password) {
}
