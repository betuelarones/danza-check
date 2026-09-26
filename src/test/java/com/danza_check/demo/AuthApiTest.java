package com.danza_check.demo;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Autenticación del administrador")
class AuthApiTest extends ApiTestBase {

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	@DisplayName("Login correcto devuelve token JWT y datos del administrador")
	void loginCorrectoDevuelveToken() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeLogin("admin-test", "password-test")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").isNumber())
			.andExpect(jsonPath("$.username").value("admin-test"));
	}

	@Test
	@DisplayName("Login con contraseña incorrecta devuelve 401")
	void loginConContrasenaIncorrectaDevuelve401() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeLogin("admin-test", "otra-contrasena")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.message").isNotEmpty())
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	@DisplayName("Login con usuario desconocido devuelve 401")
	void loginConUsuarioDesconocidoDevuelve401() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeLogin("otro-admin", "password-test")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Login sin datos devuelve 400 de validación")
	void loginSinDatosDevuelve400() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").isNotEmpty());
	}

	@Test
	@DisplayName("GET /api/auth/me sin token devuelve 401")
	void meSinTokenDevuelve401() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	@DisplayName("GET /api/auth/me con token devuelve el administrador y nunca la contraseña")
	void meConTokenDevuelveAdministrador() throws Exception {
		mockMvc.perform(get("/api/auth/me").header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("admin-test"))
			.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	@DisplayName("POST /api/auth/logout con token devuelve 204")
	void logoutConTokenDevuelve204() throws Exception {
		mockMvc.perform(post("/api/auth/logout").header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("POST /api/auth/logout sin token devuelve 401")
	void logoutSinTokenDevuelve401() throws Exception {
		mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Login repetido sigue funcionando tras autenticar una vez")
	void loginsRepetidosSiguenFuncionando() throws Exception {
		// Regresion: Spring Security borra las credenciales del UserDetails
		// devuelto tras cada autenticacion, asi que el hash no puede
		// compartirse en una unica instancia.
		for (int intento = 0; intento < 3; intento++) {
			mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
					.content(cuerpoDeLogin("admin-test", "password-test")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());
		}
	}

	@Test
	@DisplayName("GET /api/auth/me con token corrupto devuelve 401 en JSON")
	void meConTokenCorruptoDevuelve401EnJson() throws Exception {
		mockMvc.perform(get("/api/auth/me").header(cabeceraDeAutorizacion(), "Bearer token-corrupto"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.message").isNotEmpty())
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	@DisplayName("GET /api/auth/me con token caducado devuelve 401 en JSON")
	void meConTokenCaducadoDevuelve401EnJson() throws Exception {
		Instant ahora = Instant.now();
		String tokenCaducado = jwtEncoder
			.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
				JwtClaimsSet.builder()
					.issuer("danzacheck-test")
					.subject("admin-test")
					.issuedAt(ahora.minus(Duration.ofHours(2)))
					.expiresAt(ahora.minus(Duration.ofHours(1)))
					.claim("rol", "ADMIN")
					.build()))
			.getTokenValue();

		mockMvc.perform(get("/api/auth/me").header(cabeceraDeAutorizacion(), "Bearer " + tokenCaducado))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.message").isNotEmpty());
	}

}
