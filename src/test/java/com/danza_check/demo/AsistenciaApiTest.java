package com.danza_check.demo;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.danza_check.demo.entity.Asistencia;
import com.danza_check.demo.entity.SesionAsistencia;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Asistencias de los alumnos")
class AsistenciaApiTest extends ApiTestBase {

	private static final String NOMBRE = "Betuel Arones";
	private static final String CORREO = "betuel@ejemplo.com";

	@Test
	@DisplayName("Registrar asistencia es público y devuelve 201")
	void registrarAsistenciaNoRequiereAutenticacion() throws Exception {
		String codigo = crearSesionYDevolverCodigo();

		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia(NOMBRE, CORREO)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.nombre").value(NOMBRE))
			.andExpect(jsonPath("$.correo").value(CORREO))
			.andExpect(jsonPath("$.fechaHora").isNotEmpty());
	}

	@Test
	@DisplayName("El correo se normaliza a minúsculas")
	void registrarAsistenciaNormalizaElCorreo() throws Exception {
		String codigo = crearSesionYDevolverCodigo();

		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia(NOMBRE, "BETUEL@Ejemplo.COM")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.correo").value("betuel@ejemplo.com"));
	}

	@Test
	@DisplayName("Una segunda asistencia del mismo correo en la misma sesión devuelve 409")
	void registrarAsistenciaDuplicadaDevuelve409() throws Exception {
		String codigo = crearSesionYDevolverCodigo();

		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia(NOMBRE, CORREO)))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia(NOMBRE, CORREO)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.message").value(Matchers.containsString("ya fue registrada")))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	@DisplayName("El duplicado se detecta ignorando mayúsculas y minúsculas del correo")
	void registrarAsistenciaDuplicadaIgnoraCapitalizacionDevuelve409() throws Exception {
		String codigo = crearSesionYDevolverCodigo();

		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia(NOMBRE, "juan@ejemplo.com")))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia("Juan Perez", "Juan@Ejemplo.com")))
			.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("La restricción única de la base de datos también rechaza el duplicado")
	void laBaseDeDatosRechazaCorreosDuplicadosEnLaMismaSesion() {
		SesionAsistencia sesion = sesionRepository.save(new SesionAsistencia("Ensayo de danza", LocalDate.of(2026, 9, 25),
			LocalTime.of(19, 0), LocalTime.of(21, 0), "ABC234"));
		asistenciaRepository.saveAndFlush(new Asistencia(NOMBRE, "betuel@ejemplo.com", LocalDateTime.now(), sesion));

		Asistencia duplicada = new Asistencia(NOMBRE, "betuel@ejemplo.com", LocalDateTime.now(), sesion);

		assertThatThrownBy(() -> asistenciaRepository.saveAndFlush(duplicada))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("El mismo correo puede asistir en sesiones distintas")
	void elMismoCorreoPuedeAsistirEnVariasSesiones() throws Exception {
		crearSesionYDevolverCodigo();
		String segundoCodigo = crearSesionYDevolverCodigo();

		for (String codigo : new String[] { crearSesionYDevolverCodigo(), segundoCodigo }) {
			mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
					.contentType(MediaType.APPLICATION_JSON)
					.content(cuerpoDeAsistencia(NOMBRE, CORREO)))
				.andExpect(status().isCreated());
		}
	}

	@Test
	@DisplayName("No se puede registrar asistencia en una sesión cerrada")
	void registrarAsistenciaEnSesionCerradaDevuelve409() throws Exception {
		long id = idDeSesionCreada();
		mockMvc.perform(patch("/api/sesiones/" + id + "/cerrar")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk());
		String codigo = sesionRepository.findById(id).orElseThrow().getCodigo();

		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia(NOMBRE, CORREO)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	@DisplayName("Un código de sesión inexistente devuelve 404")
	void registrarAsistenciaConCodigoInexistenteDevuelve404() throws Exception {
		mockMvc.perform(post("/api/sesiones/ZZZZZZ/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia(NOMBRE, CORREO)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@DisplayName("Un correo con formato inválido devuelve 400")
	void registrarAsistenciaConCorreoInvalidoDevuelve400() throws Exception {
		String codigo = crearSesionYDevolverCodigo();

		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia(NOMBRE, "correo-no-valido")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").isNotEmpty());
	}

	@Test
	@DisplayName("Nombre o correo vacíos devuelven 400")
	void registrarAsistenciaSinDatosDevuelve400() throws Exception {
		String codigo = crearSesionYDevolverCodigo();

		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nombre": "  ",
					  "correo": ""
					}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@DisplayName("Listar asistencias requiere autenticación")
	void listarAsistenciasSinAutenticacionDevuelve401() throws Exception {
		long id = idDeSesionCreada();

		mockMvc.perform(get("/api/sesiones/" + id + "/asistencias")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Listar asistencias devuelve las asistencias de la sesión")
	void listarAsistenciasDevuelve200() throws Exception {
		String codigo = crearSesionYDevolverCodigo();
		long id = sesionRepository.findByCodigo(codigo).orElseThrow().getId();
		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia(NOMBRE, CORREO)))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/sesiones/" + id + "/asistencias")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].nombre").value(NOMBRE))
			.andExpect(jsonPath("$[0].correo").value(CORREO));
	}

	@Test
	@DisplayName("Listar asistencias de una sesión inexistente devuelve 404")
	void listarAsistenciasDeSesionInexistenteDevuelve404() throws Exception {
		mockMvc.perform(get("/api/sesiones/999/asistencias")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Contar asistentes devuelve solo la cantidad")
	void contarAsistenciasDevuelveCantidad() throws Exception {
		String codigo = crearSesionYDevolverCodigo();
		long id = sesionRepository.findByCodigo(codigo).orElseThrow().getId();
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
					.contentType(MediaType.APPLICATION_JSON)
					.content(cuerpoDeAsistencia("Alumno " + i, "alumno" + i + "@ejemplo.com")))
				.andExpect(status().isCreated());
		}

		mockMvc.perform(get("/api/sesiones/" + id + "/asistencias/count")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cantidad").value(3))
			.andExpect(jsonPath("$.nombre").doesNotExist());
	}

	@Test
	@DisplayName("Contar asistentes de una sesión inexistente devuelve 404")
	void contarAsistenciasDeSesionInexistenteDevuelve404() throws Exception {
		mockMvc.perform(get("/api/sesiones/999/asistencias/count")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("La lista de asistencias queda ordenada por fecha de registro")
	void listarAsistenciasDevuelveElOrdenDeRegistro() throws Exception {
		String codigo = crearSesionYDevolverCodigo();
		long id = sesionRepository.findByCodigo(codigo).orElseThrow().getId();
		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia("Primero", "primero@ejemplo.com")))
			.andExpect(status().isCreated());
		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia("Segundo", "segundo@ejemplo.com")))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/sesiones/" + id + "/asistencias")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].nombre").value("Primero"))
			.andExpect(jsonPath("$[1].nombre").value("Segundo"));
	}

	@Test
	@DisplayName("Las asistencias de una sesión no se mezclan con las de otra")
	void lasAsistenciasSonPropiasDeCadaSesion() throws Exception {
		String primerCodigo = crearSesionYDevolverCodigo();
		String segundoCodigo = crearSesionYDevolverCodigo();
		long segundoId = sesionRepository.findByCodigo(segundoCodigo).orElseThrow().getId();
		mockMvc.perform(post("/api/sesiones/" + primerCodigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia(NOMBRE, CORREO)))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/sesiones/" + segundoId + "/asistencias")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("El código se normaliza: minúsculas y espacios funcionan igual")
	void elCodigoSeNormalizaAlRegistrar() throws Exception {
		String codigo = crearSesionYDevolverCodigo();

		mockMvc.perform(post("/api/sesiones/" + codigo.toLowerCase() + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia(NOMBRE, CORREO)))
			.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("Un cuerpo con JSON inválido devuelve 400")
	void registrarAsistenciaConCuerpoInvalidoDevuelve400() throws Exception {
		String codigo = crearSesionYDevolverCodigo();

		mockMvc.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{nombre"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@DisplayName("Borrar una asistencia requiere autenticación")
	void eliminarAsistenciaSinAutenticacionDevuelve401() throws Exception {
		long id = idDeSesionCreada();
		long asistenciaId = registrarYDevolverIdDeAsistencia(id);

		mockMvc.perform(delete("/api/sesiones/" + id + "/asistencias/" + asistenciaId))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Borrar una asistencia devuelve 204 y la quita del listado")
	void eliminarAsistenciaDevuelve204() throws Exception {
		long id = idDeSesionCreada();
		long asistenciaId = registrarYDevolverIdDeAsistencia(id);

		mockMvc.perform(delete("/api/sesiones/" + id + "/asistencias/" + asistenciaId)
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/sesiones/" + id + "/asistencias")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Borrar una asistencia baja el conteo")
	void eliminarAsistenciaBajaElConteo() throws Exception {
		long id = idDeSesionCreada();
		long asistenciaId = registrarYDevolverIdDeAsistencia(id);
		registrarYDevolverIdDeAsistencia(id, "segunda@ejemplo.com");

		mockMvc.perform(delete("/api/sesiones/" + id + "/asistencias/" + asistenciaId)
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/sesiones/" + id + "/asistencias/count")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cantidad").value(1));
	}

	@Test
	@DisplayName("Borrar solo quita la asistencia indicada y deja las demás")
	void eliminarAsistenciaNoTocaLasDemas() throws Exception {
		long id = idDeSesionCreada();
		long asistenciaId = registrarYDevolverIdDeAsistencia(id);
		registrarYDevolverIdDeAsistencia(id, "otra@ejemplo.com");

		mockMvc.perform(delete("/api/sesiones/" + id + "/asistencias/" + asistenciaId)
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/sesiones/" + id + "/asistencias")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].correo").value("otra@ejemplo.com"));
	}

	@Test
	@DisplayName("No se puede borrar desde una sesión ajena la asistencia de otra")
	void eliminarAsistenciaDeOtraSesionDevuelve404() throws Exception {
		long idConLaAsistencia = idDeSesionCreada();
		long asistenciaId = registrarYDevolverIdDeAsistencia(idConLaAsistencia);
		long otraSesionId = idDeSesionCreada();

		mockMvc.perform(delete("/api/sesiones/" + otraSesionId + "/asistencias/" + asistenciaId)
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404));

		// La asistencia sigue viva: el 404 no la habia tocado.
		mockMvc.perform(get("/api/sesiones/" + idConLaAsistencia + "/asistencias")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	@DisplayName("Borrar una asistencia inexistente devuelve 404")
	void eliminarAsistenciaInexistenteDevuelve404() throws Exception {
		long id = idDeSesionCreada();

		mockMvc.perform(delete("/api/sesiones/" + id + "/asistencias/999999")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@DisplayName("Borrar en una sesión inexistente devuelve 404")
	void eliminarAsistenciaDeSesionInexistenteDevuelve404() throws Exception {
		mockMvc.perform(delete("/api/sesiones/999/asistencias/1")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Se puede borrar de una sesión ya cerrada")
	void eliminarAsistenciaDeSesionCerrada() throws Exception {
		long id = idDeSesionCreada();
		long asistenciaId = registrarYDevolverIdDeAsistencia(id);
		mockMvc.perform(patch("/api/sesiones/" + id + "/cerrar")
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isOk());

		mockMvc.perform(delete("/api/sesiones/" + id + "/asistencias/" + asistenciaId)
				.header(cabeceraDeAutorizacion(), "Bearer " + tokenAdmin()))
			.andExpect(status().isNoContent());
	}

	/** Registra una asistencia en la sesión dada y devuelve su id. */
	private long registrarYDevolverIdDeAsistencia(long sesionId) throws Exception {
		return registrarYDevolverIdDeAsistencia(sesionId, CORREO);
	}

	/**
	 * Igual que el anterior pero con el correo indicado: hace falta cuando la
	 * prueba necesita dos asistencias en la misma sesión, porque el correo es
	 * único por sesión.
	 */
	private long registrarYDevolverIdDeAsistencia(long sesionId, String correo) throws Exception {
		String codigo = sesionRepository.findById(sesionId).orElseThrow().getCodigo();
		MvcResult resultado = mockMvc
			.perform(post("/api/sesiones/" + codigo + "/asistencias")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoDeAsistencia(NOMBRE, correo)))
			.andExpect(status().isCreated())
			.andReturn();
		return jsonMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8))
			.get("id")
			.asLong();
	}

}
