package com.danza_check.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Sesiones de asistencia")
class SesionApiTest extends ApiTestBase {

	@Test
	@DisplayName("Crear sesión devuelve 201, activa y con código generado de 6 caracteres")
	void crearSesionDevuelveCodigoGenerado() throws Exception {
		mockMvc.perform(post("/api/sesiones").header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin())
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeSesion()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.nombre").value("Ensayo de danza"))
			.andExpect(jsonPath("$.fecha").value("2026-09-25"))
			.andExpect(jsonPath("$.horaInicio").value("19:00:00"))
			.andExpect(jsonPath("$.horaFin").value("21:00:00"))
			.andExpect(jsonPath("$.codigo").isNotEmpty())
			.andExpect(jsonPath("$.activa").value(true));
	}

	@Test
	@DisplayName("El código generado es único para cada sesión")
	void losCodigosGeneradosSonUnicos() throws Exception {
		String primero = crearSesionYDevolverCodigo();
		String segundo = crearSesionYDevolverCodigo();

		assertThat(primero).hasSize(6).isNotEqualTo(segundo);
	}

	@Test
	@DisplayName("Crear sesión sin autenticación devuelve 401")
	void crearSesionSinAutenticacionDevuelve401() throws Exception {
		mockMvc.perform(post("/api/sesiones").contentType(MediaType.APPLICATION_JSON).content(cuerpoDeSesion()))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Crear sesión con token inválido devuelve 401")
	void crearSesionConTokenInvalidoDevuelve401() throws Exception {
		mockMvc.perform(post("/api/sesiones").header(cabeceraDeAutorizacion(), "Bearer token-falso")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeSesion()))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Crear sesión sin nombre o sin fecha devuelve 400 de validación")
	void crearSesionConDatosInvalidosDevuelve400() throws Exception {
		mockMvc.perform(post("/api/sesiones").header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "fecha": "2026-09-25",
					  "horaInicio": "19:00"
					}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").isNotEmpty());
	}

	@Test
	@DisplayName("Crear sesión con hora de fin anterior a la de inicio devuelve 400")
	void crearSesionConHoraFinInvalidaDevuelve400() throws Exception {
		mockMvc.perform(post("/api/sesiones").header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nombre": "Ensayo de danza",
					  "fecha": "2026-09-25",
					  "horaInicio": "19:00",
					  "horaFin": "18:00"
					}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").isNotEmpty());
	}

	@Test
	@DisplayName("Crear sesión con fecha inválida devuelve 400")
	void crearSesionConFechaInvalidaDevuelve400() throws Exception {
		mockMvc.perform(post("/api/sesiones").header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nombre": "Ensayo de danza",
					  "fecha": "25-09-2026",
					  "horaInicio": "19:00"
					}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@DisplayName("Listar sesiones requiere autenticación")
	void listarSesionesSinAutenticacionDevuelve401() throws Exception {
		mockMvc.perform(get("/api/sesiones")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Listar sesiones devuelve las sesiones existentes")
	void listarSesionesDevuelve200() throws Exception {
		crearSesion();

		mockMvc.perform(get("/api/sesiones").header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].codigo").isNotEmpty());
	}

	@Test
	@DisplayName("Obtener sesión por id devuelve 200")
	void obtenerSesionPorIdDevuelve200() throws Exception {
		long id = idDeSesionCreada();

		mockMvc.perform(get("/api/sesiones/" + id).header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.activa").value(true));
	}

	@Test
	@DisplayName("Obtener sesión inexistente devuelve 404")
	void obtenerSesionInexistenteDevuelve404() throws Exception {
		mockMvc.perform(get("/api/sesiones/999").header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@DisplayName("Consultar una sesión por código es público y devuelve solo datos mínimos")
	void obtenerSesionPorCodigoEsPublico() throws Exception {
		String codigo = crearSesionYDevolverCodigo();

		mockMvc.perform(get("/api/sesiones/codigo/" + codigo))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value("Ensayo de danza"))
			.andExpect(jsonPath("$.fecha").value("2026-09-25"))
			.andExpect(jsonPath("$.activa").value(true))
			.andExpect(jsonPath("$.codigo").doesNotExist())
			.andExpect(jsonPath("$.createdAt").doesNotExist());
	}

	@Test
	@DisplayName("Consultar un código inexistente devuelve 404")
	void obtenerSesionPorCodigoInexistenteDevuelve404() throws Exception {
		mockMvc.perform(get("/api/sesiones/codigo/ZZZZZZ"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@DisplayName("Cerrar sesión requiere autenticación")
	void cerrarSesionSinAutenticacionDevuelve401() throws Exception {
		long id = idDeSesionCreada();

		mockMvc.perform(patch("/api/sesiones/" + id + "/cerrar")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Cerrar sesión marca la sesión como inactiva")
	void cerrarSesionDesactivaLaSesion() throws Exception {
		long id = idDeSesionCreada();

		mockMvc.perform(patch("/api/sesiones/" + id + "/cerrar")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.activa").value(false));

		mockMvc.perform(get("/api/sesiones/" + id).header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.activa").value(false));
	}

	@Test
	@DisplayName("Eliminar sesión requiere autenticación")
	void eliminarSesionSinAutenticacionDevuelve401() throws Exception {
		long id = idDeSesionCreada();

		mockMvc.perform(delete("/api/sesiones/" + id)).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Eliminar sesión devuelve 204 y la quita del listado")
	void eliminarSesionDevuelve204() throws Exception {
		long id = idDeSesionCreada();

		mockMvc.perform(delete("/api/sesiones/" + id)
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/sesiones/" + id)
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Eliminar sesión inexistente devuelve 404")
	void eliminarSesionInexistenteDevuelve404() throws Exception {
		mockMvc.perform(delete("/api/sesiones/999")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@DisplayName("Eliminar una sesión se lleva también sus asistencias")
	void eliminarSesionBorraSusAsistencias() throws Exception {
		long id = idDeSesionCreada();
		String codigo = sesionRepository.findById(id).orElseThrow().getCodigo();
		for (int i = 0; i < 2; i++) {
			mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
					.contentType(MediaType.APPLICATION_JSON)
					.content(cuerpoDeAsistencia("Alumno " + i, "alumno" + i + "@ejemplo.com")))
				.andExpect(status().isCreated());
		}
		assertThat(asistenciaRepository.countBySesionId(id)).isEqualTo(2);

		mockMvc.perform(delete("/api/sesiones/" + id)
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNoContent());

		assertThat(asistenciaRepository.findBySesionIdOrderByFechaHoraAscIdAsc(id)).isEmpty();
		assertThat(sesionRepository.findById(id)).isEmpty();
	}

	@Test
	@DisplayName("Eliminar una sesión no toca las asistencias de otra")
	void eliminarSesionNoTocaLasAsistenciasDeOtra() throws Exception {
		long idAEliminar = idDeSesionCreada();
		long idQueSeQueda = idDeSesionCreada();
		String codigoQueSeQueda = sesionRepository.findById(idQueSeQueda).orElseThrow().getCodigo();
		mockMvc.perform(post("/api/sesiones/" + codigoQueSeQueda + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia("Se queda", "sequeda@ejemplo.com")))
			.andExpect(status().isCreated());

		mockMvc.perform(delete("/api/sesiones/" + idAEliminar)
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNoContent());

		assertThat(asistenciaRepository.countBySesionId(idQueSeQueda)).isEqualTo(1);
	}

	@Test
	@DisplayName("El código de una sesión eliminada vuelve a quedar libre")
	void elCodigoDeUnaSesionEliminadaSeLibera() throws Exception {
		long id = idDeSesionCreada();
		String codigo = sesionRepository.findById(id).orElseThrow().getCodigo();

		mockMvc.perform(delete("/api/sesiones/" + id)
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNoContent());

		// Si el código quedara ocupado, volver a insertarlo violaría la
		// restricción única.
		mockMvc.perform(delete("/api/sesiones/" + id)
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNotFound());
		assertThat(sesionRepository.findByCodigo(codigo)).isEmpty();
	}

	@Test
	@DisplayName("Después de eliminar, el QR del enlace público da 404")
	void elEnlacePublicoDeUnaSesionEliminadaDa404() throws Exception {
		long id = idDeSesionCreada();
		String codigo = sesionRepository.findById(id).orElseThrow().getCodigo();

		mockMvc.perform(delete("/api/sesiones/" + id)
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/sesiones/codigo/" + codigo))
			.andExpect(status().isNotFound());
	}

}
