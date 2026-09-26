package com.danza_check.demo.exception;

import org.springframework.http.HttpStatus;

public class CodigoGeneracionException extends ApiException {

	public CodigoGeneracionException(String message) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, message);
	}

}
