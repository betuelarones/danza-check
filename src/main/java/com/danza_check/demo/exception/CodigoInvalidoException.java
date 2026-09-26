package com.danza_check.demo.exception;

/**
 * El código de sesión indicado no existe. Se responde 404 porque el
 * recurso (la sesión) no existe.
 */
public class CodigoInvalidoException extends NotFoundException {

	public CodigoInvalidoException(String codigo) {
		super("El código de sesión indicado no es válido.");
	}

}
