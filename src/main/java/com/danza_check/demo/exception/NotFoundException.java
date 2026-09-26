package com.danza_check.demo.exception;

import org.springframework.http.HttpStatus;

public abstract class NotFoundException extends ApiException {

	protected NotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, message);
	}

}
