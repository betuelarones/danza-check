package com.danza_check.demo;

import java.nio.charset.StandardCharsets;

import com.danza_check.demo.repository.AsistenciaRepository;
import com.danza_check.demo.repository.SesionAsistenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base de las pruebas de la API. Las pruebas usan el perfil "test" (H2 en
 * memoria) y se autentican contra el endpoint real de login, de modo que
 * el flujo de autenticación por JWT queda verificado de punta a punta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiTestBase {

	private static final String LOGIN = "/api/auth/login";
	private static final String SESIONES = "/api/sesiones";
	private static final String NOMBRE_SESION = "Ensayo de danza";
	private static final String FECHA_SESION = "2026-09-25";
	private static final String HORA_INICIO = "19:00";
	private static final String HORA_FIN = "21:00";

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected JsonMapper jsonMapper;

	@Autowired
	protected AsistenciaRepository asistenciaRepository;

	@Autowired
	protected SesionAsistenciaRepository sesionRepository;

	@Value("${app.security.admin.username}")
	protected String adminUsername;

	@Value("${app.security.admin.password}")
	protected String adminPassword;

	@BeforeEach
	void limpiarBaseDeDatos() {
		asistenciaRepository.deleteAll();
		sesionRepository.deleteAll();
	}

	protected String cuerpoDeLogin(String usuario, String contrasena) {
		return """
			{
			  "username": "%s",
			  "password": "%s"
			}""".formatted(usuario, contrasena);
	}

	protected String cuerpoDeSesion() {
		return """
			{
			  "nombre": "%s",
			  "fecha": "%s",
			  "horaInicio": "%s",
			  "horaFin": "%s"
			}""".formatted(NOMBRE_SESION, FECHA_SESION, HORA_INICIO, HORA_FIN);
	}

	protected String cuerpoDeAsistencia(String nombre, String correo) {
		return """
			{
			  "nombre": "%s",
			  "correo": "%s"
			}""".formatted(nombre, correo);
	}

	/**
	 * Inicia sesión con el administrador del perfil de pruebas y devuelve el
	 * token JWT.
	 */
	protected String tokenAdmin() throws Exception {
		MvcResult resultado = mockMvc
			.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeLogin(adminUsername, adminPassword)))
			.andReturn();
		String cuerpo = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertThat(resultado.getResponse().getStatus())
			.as("Login del administrador. Respuesta recibida: %s", cuerpo)
			.isEqualTo(HttpStatus.OK.value());
		return jsonMapper.readTree(cuerpo).get("token").asText();
	}

	protected String cabeceraDeAutorizacion() {
		return HttpHeaders.AUTHORIZATION;
	}

	/**
	 * Crea una sesión desde la API y devuelve su código.
	 */
	protected String crearSesionYDevolverCodigo() throws Exception {
		return crearSesion().get("codigo").asText();
	}

	/**
	 * Crea una sesión desde la API y devuelve el JSON de la respuesta.
	 */
	protected tools.jackson.databind.JsonNode crearSesion() throws Exception {
		MvcResult resultado = mockMvc
			.perform(post(SESIONES).header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin())
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeSesion()))
			.andReturn();
		String cuerpo = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertThat(resultado.getResponse().getStatus())
			.as("Creación de sesión. Respuesta recibida: %s", cuerpo)
			.isEqualTo(HttpStatus.CREATED.value());
		return jsonMapper.readTree(cuerpo);
	}

	protected long idDeSesionCreada() throws Exception {
		return crearSesion().get("id").asLong();
	}

}
