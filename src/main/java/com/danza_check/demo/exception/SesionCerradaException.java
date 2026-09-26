package com.danza_check.demo.exception;

import org.springframework.http.HttpStatus;

public class SesionCerradaException extends ApiException {

	public SesionCerradaException(String message) {
		super(HttpStatus.CONFLICT, message);
	}

}
