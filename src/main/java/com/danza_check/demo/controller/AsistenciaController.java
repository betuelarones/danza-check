package com.danza_check.demo.controller;

import java.util.List;

import com.danza_check.demo.dto.AsistenciaCountResponse;
import com.danza_check.demo.dto.AsistenciaResponse;
import com.danza_check.demo.dto.RegistrarAsistenciaRequest;
import com.danza_check.demo.service.AsistenciaService;
import com.danza_check.demo.service.AsistenciaStreamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sesiones")
public class AsistenciaController {

	private final AsistenciaService asistenciaService;

	private final AsistenciaStreamService streamService;

	public AsistenciaController(AsistenciaService asistenciaService, AsistenciaStreamService streamService) {
		this.asistenciaService = asistenciaService;
		this.streamService = streamService;
	}

	/**
	 * Endpoint público: el alumno se identifica con el código de la sesión
	 * y su nombre y correo, sin necesidad de iniciar sesión.
	 */
	@PostMapping("/{codigo}/asistencias")
	public ResponseEntity<AsistenciaResponse> registrar(@PathVariable String codigo,
			@Valid @RequestBody RegistrarAsistenciaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(asistenciaService.registrar(codigo, request));
	}

	@GetMapping("/{id}/asistencias")
	public List<AsistenciaResponse> listar(@PathVariable Long id) {
		return asistenciaService.listarPorSesion(id);
	}

	@GetMapping("/{id}/asistencias/count")
	public AsistenciaCountResponse contar(@PathVariable Long id) {
		return asistenciaService.contarPorSesion(id);
	}

	/**
	 * Borra una asistencia puntual del listado. Es una corrección manual, no
	 * un cierre de sesión: la sesión sigue existiendo y el resto de la lista
	 * no se toca.
	 */
	@DeleteMapping("/{id}/asistencias/{asistenciaId}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id, @PathVariable Long asistenciaId) {
		asistenciaService.eliminar(id, asistenciaId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Stream de la sesion: el panel abierto recibe cada asistencia nueva
	 * sin tener que recargar.
	 *
	 * <p>Eventos emitidos:
	 * <ul>
	 * <li>{@code conectado}, con el conteo actual, al abrir la conexion;</li>
	 * <li>{@code asistencia.registrada}, con la asistencia y el conteo, cada
	 * vez que alguien se apunta;</li>
	 * <li>{@code sesion.cerrada}, con la sesion actualizada, y despues el
	 * servidor cierra la conexion.</li>
	 * </ul>
	 *
	 * <p>Los latidos llegan como comentarios SSE y el cliente los ignora:
	 * solo sirven para que la linea no se cierre por inactividad.
	 *
	 * <p>Devuelve 404 si la sesion no existe, para no dejar abierta una
	 * conexion que nunca va a recibir nada.
	 */
	@GetMapping(value = "/{id}/asistencias/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@PathVariable Long id) {
		return streamService.suscribir(id, asistenciaService.contarPorSesion(id).cantidad());
	}

}
