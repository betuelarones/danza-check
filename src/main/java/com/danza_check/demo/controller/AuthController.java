package com.danza_check.demo.controller;

import com.danza_check.demo.dto.AdminResponse;
import com.danza_check.demo.dto.LoginRequest;
import com.danza_check.demo.dto.LoginResponse;
import com.danza_check.demo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	/**
	 * El backend es stateless: el cierre de sesión consiste en descartar el
	 * token en el cliente. El endpoint se mantiene protegido y responde 204.
	 */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public AdminResponse me(Authentication authentication) {
		return authService.infoDelAdministrador(authentication.getName());
	}

}
