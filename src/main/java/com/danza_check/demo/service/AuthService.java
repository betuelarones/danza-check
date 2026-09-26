package com.danza_check.demo.service;

import com.danza_check.demo.dto.AdminResponse;
import com.danza_check.demo.dto.LoginRequest;
import com.danza_check.demo.dto.LoginResponse;
import com.danza_check.demo.exception.CredencialesInvalidasException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Autenticacion del unico administrador. No se registran contrasenas ni
 * se guardan en logs: la comparacion la hace Spring Security contra las
 * credenciales recibidas por variable de entorno.
 */
@Service
@Slf4j
public class AuthService {

	private static final String TIPO_TOKEN = "Bearer";
	private static final String ROL_ADMIN = "ADMIN";

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	public LoginResponse login(LoginRequest request) {
		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.username().trim(), request.password()));
		}
		catch (AuthenticationException ex) {
			log.debug("La autenticacion de '{}' fallo: {}", request.username(), ex.toString());
			throw new CredencialesInvalidasException();
		}
		// El login es programático, así que el contexto de seguridad de la
		// petición se establece a mano. No se persiste en servidor porque la
		// autenticación es stateless: el token es lo que viaja al cliente.
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);

		String username = authentication.getName();
		return new LoginResponse(jwtService.generarToken(username), TIPO_TOKEN, jwtService.expiracionEnSegundos(),
			username);
	}

	public AdminResponse infoDelAdministrador(String username) {
		return new AdminResponse(username, ROL_ADMIN);
	}

}
