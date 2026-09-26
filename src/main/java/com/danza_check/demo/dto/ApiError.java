package com.danza_check.demo.dto;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

/**
 * Formato unico de error de la API. Nunca incluye stack traces ni datos
 * sensibles.
 */
public record ApiError(int status, String message, LocalDateTime timestamp) {

	public static ApiError of(HttpStatus status, String message) {
		return new ApiError(status.value(), message, LocalDateTime.now());
	}

}
