package com.danza_check.demo;

import com.danza_check.demo.dto.AsistenciaRegistradaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.danza_check.demo.service.AsistenciaStreamService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lo que el panel recibe por el stream cuando alguien se apunta.
 *
 * <p>El punto delicado es el momento: el evento se publica despues del
 * commit, no durante la transaccion. Si se publicara antes, el panel
 * veria asistencias que la base de datos despues rechaza.
 */
@DisplayName("Notificación de asistencias al panel")
class NotificacionStreamTest extends ApiTestBase {

	private static final String NOMBRE = "Betuel Arones";

	private static final String CORREO = "betuel@ejemplo.com";

	@MockitoSpyBean
	private AsistenciaStreamService streamService;

	@Test
	@DisplayName("Una asistencia confirmada llega al panel con la asistencia y el conteo")
	void unaAsistenciaConfirmadaNotificaAlPanel() throws Exception {
		String codigo = crearSesionYDevolverCodigo();
		long id = sesionRepository.findByCodigo(codigo).orElseThrow().getId();

		registrar(codigo, CORREO);

		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(streamService).emitir(eq(id), eq("asistencia.registrada"), captor.capture());
		AsistenciaRegistradaResponse payload = (AsistenciaRegistradaResponse) captor.getValue();
		assertThat(payload.asistencia().nombre()).isEqualTo(NOMBRE);
		assertThat(payload.asistencia().correo()).isEqualTo(CORREO);
		assertThat(payload.cantidad()).isEqualTo(1L);
	}

	@Test
	@DisplayName("Un correo duplicado no genera ninguna notificación")
	void unDuplicadoNoNotificaAlPanel() throws Exception {
		String codigo = crearSesionYDevolverCodigo();
		long id = sesionRepository.findByCodigo(codigo).orElseThrow().getId();
		registrar(codigo, CORREO);
		// Se descartan las llamadas del primer registro para poder
		// afirmar sobre la segunda peticion sola.
		Mockito.clearInvocations(streamService);

		registrarDuplicado(codigo, CORREO);

		// La segunda peticion falla con 409 y su transaccion revierte, asi
		// que el panel no debe enterarse de nada.
		verify(streamService, never()).emitir(eq(id), eq("asistencia.registrada"), any());
	}

	@Test
	@DisplayName("Cerrar la sesión avisa al panel y cierra el stream")
	void cerrarSesionNotificaAlPanel() throws Exception {
		String codigo = crearSesionYDevolverCodigo();
		long id = sesionRepository.findByCodigo(codigo).orElseThrow().getId();
		registrar(codigo, CORREO);
		Mockito.clearInvocations(streamService);

		mockMvc.perform(patch("/api/sesiones/" + id + "/cerrar")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk());

		verify(streamService).emitir(eq(id), eq("sesion.cerrada"), any());
		verify(streamService).cerrar(id);
	}

	private void registrar(String codigo, String correo) throws Exception {
		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia(NOMBRE, correo)))
			.andExpect(status().isCreated());
	}

	/** El mismo correo otra vez: lo rechaza la restriccion unica. */
	private void registrarDuplicado(String codigo, String correo) throws Exception {
		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia(NOMBRE, correo)))
			.andExpect(status().isConflict());
	}

}
