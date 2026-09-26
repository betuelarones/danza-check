package com.danza_check.demo.dto;

/**
 * Respuesta de login: token JWT y datos minimos del administrador.
 * Nunca incluye la contrasena.
 */
public record LoginResponse(String token, String tokenType, long expiresIn, String username) {
}
