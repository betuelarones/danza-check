package com.danza_check.demo.dto;

/**
 * Informacion del administrador autenticado. No expone la contrasena.
 */
public record AdminResponse(String username, String rol) {
}
