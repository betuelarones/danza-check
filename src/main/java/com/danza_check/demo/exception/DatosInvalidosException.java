package com.danza_check.demo.exception;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio en los datos enviados (HTTP 400).
 */
public class DatosInvalidosException extends ApiException {

	public DatosInvalidosException(String message) {
		super(HttpStatus.BAD_REQUEST, message);
	}

}
