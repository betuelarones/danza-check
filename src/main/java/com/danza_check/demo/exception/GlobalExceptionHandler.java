package com.danza_check.demo.exception;

import java.util.stream.Collectors;

import com.danza_check.demo.dto.ApiError;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Manejo global de errores. Todas las respuestas de error usan el formato
 * {status, message, timestamp} y nunca incluyen stack traces.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String MENSAJE_ERROR_GENERAL = "Ocurrió un error inesperado. Intenta nuevamente.";
	private static final String MENSAJE_JSON_INVALIDO = "El cuerpo de la petición no tiene un formato válido.";
	private static final String MENSAJE_METODO = "El método HTTP no está permitido para este recurso.";
	private static final String MENSAJE_NO_ENCONTRADO = "El recurso solicitado no existe.";
	private static final String MENSAJE_CONFLICTO_DATOS = "La operación entra en conflicto con los datos existentes.";

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiError> manejarApiException(ApiException ex) {
		log.debug("Error de negocio {}: {}", ex.getStatus().value(), ex.getMessage());
		return responder(ex.getStatus(), ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> manejarValidacion(MethodArgumentNotValidException ex) {
		String detalle = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(GlobalExceptionHandler::describirCampo)
			.sorted()
			.collect(Collectors.joining("; "));
		log.debug("Datos inválidos: {}", detalle);
		return responder(HttpStatus.BAD_REQUEST, detalle.isEmpty() ? "Los datos enviados no son válidos." : detalle);
	}

	@ExceptionHandler({ HandlerMethodValidationException.class, ConstraintViolationException.class })
	ResponseEntity<ApiError> manejarValidacionDeParametros(Exception ex) {
		log.debug("Parámetros inválidos: {}", ex.getMessage());
		return responder(HttpStatus.BAD_REQUEST, "Los parámetros enviados no son válidos.");
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> manejarCuerpoInvalido(HttpMessageNotReadableException ex) {
		log.debug("Cuerpo de la petición ilegible: {}", ex.getMessage());
		return responder(HttpStatus.BAD_REQUEST, MENSAJE_JSON_INVALIDO);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiError> manejarMetodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
		return responder(HttpStatus.METHOD_NOT_ALLOWED, MENSAJE_METODO);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiError> manejarRecursoNoEncontrado(NoResourceFoundException ex) {
		return responder(HttpStatus.NOT_FOUND, MENSAJE_NO_ENCONTRADO);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> manejarConflictoDeDatos(DataIntegrityViolationException ex) {
		log.warn("Restricción de integridad violada: {}", ex.getMostSpecificCause().getMessage());
		return responder(HttpStatus.CONFLICT, MENSAJE_CONFLICTO_DATOS);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> manejarErrorInesperado(Exception ex) {
		log.error("Error inesperado", ex);
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, MENSAJE_ERROR_GENERAL);
	}

	private static String describirCampo(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}

	private static ResponseEntity<ApiError> responder(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(ApiError.of(status, message));
	}

}
