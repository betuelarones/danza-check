package com.danza_check.demo.exception;

import org.springframework.http.HttpStatus;

public class CredencialesInvalidasException extends ApiException {

	public CredencialesInvalidasException() {
		super(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.");
	}

}
