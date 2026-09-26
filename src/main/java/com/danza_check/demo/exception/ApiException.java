package com.danza_check.demo.exception;

import org.springframework.http.HttpStatus;

/**
 * Base de los errores de negocio. El mensaje que se devuelve al cliente
 * es seguro de exponer: nunca contiene datos sensibles.
 */
public abstract class ApiException extends RuntimeException {

	private final HttpStatus status;

	protected ApiException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}

}
