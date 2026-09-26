package com.danza_check.demo.exception;

import java.io.IOException;

import com.danza_check.demo.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Escribe las respuestas 401 y 403 de Spring Security con el mismo
 * formato de error que el manejador global, en lugar del cuerpo vacío
 * que devuelve el framework por defecto.
 */
@Component
public class ApiErrorWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiErrorWriter.class);

	private final JsonMapper jsonMapper;

	public ApiErrorWriter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		escribir(response, HttpStatus.UNAUTHORIZED, "Autenticación requerida. Inicia sesión en el panel.");
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
			throws IOException {
		escribir(response, HttpStatus.FORBIDDEN, "No tienes permisos para acceder a este recurso.");
	}

	private void escribir(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		log.debug("Respuesta de seguridad {}: {}", status.value(), message);
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		jsonMapper.writeValue(response.getOutputStream(), ApiError.of(status, message));
	}

}
