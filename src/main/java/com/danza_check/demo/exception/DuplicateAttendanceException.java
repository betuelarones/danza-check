package com.danza_check.demo.exception;

import org.springframework.http.HttpStatus;

public class DuplicateAttendanceException extends ApiException {

	public DuplicateAttendanceException(String message) {
		super(HttpStatus.CONFLICT, message);
	}

}
