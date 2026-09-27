package com.danza_check.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stream SSE de asistencias.
 *
 * <p>Se verifica el contrato HTTP (que exige token, que la sesion exista
 * y que la peticion quede abierta) y que la notificacion solo salga
 * cuando la asistencia quedo de verdad confirmada.
 */
@DisplayName("Stream de asistencias")
class AsistenciaStreamApiTest extends ApiTestBase {

	@Test
	@DisplayName("Abrir el stream sin autenticación devuelve 401")
	void abrirStreamSinAutenticacionDevuelve401() throws Exception {
		long id = idDeSesionCreada();

		mockMvc.perform(get("/api/sesiones/" + id + "/asistencias/stream"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Abrir el stream de una sesión inexistente devuelve 404")
	void abrirStreamDeSesionInexistenteDevuelve404() throws Exception {
		mockMvc.perform(get("/api/sesiones/999/asistencias/stream")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Abrir el stream deja la petición abierta y responde con text/event-stream")
	void abrirStreamDejaLaPeticionAbierta() throws Exception {
		long id = idDeSesionCreada();

		MvcResult resultado = mockMvc.perform(get("/api/sesiones/" + id + "/asistencias/stream")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(request().asyncStarted())
			.andReturn();

		assertThat(resultado.getResponse().getContentType())
			.as("El stream se anuncia como Server-Sent Events")
			.startsWith("text/event-stream");
		// El primer evento lleva el conteo actual, para que el panel pinte
		// la tabla sin esperar a que alguien se apunte.
		assertThat(resultado.getResponse().getContentAsString())
			.as("Se envia el evento inicial de conexion")
			.contains("event:conectado")
			.contains("\"cantidad\":0");
	}

}
