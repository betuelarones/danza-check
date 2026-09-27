package com.danza_check.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import com.danza_check.demo.dto.AsistenciaCountResponse;
import com.danza_check.demo.dto.AsistenciaResponse;
import com.danza_check.demo.dto.RegistrarAsistenciaRequest;
import com.danza_check.demo.entity.Asistencia;
import com.danza_check.demo.entity.SesionAsistencia;
import com.danza_check.demo.evento.AsistenciaRegistradaEvento;
import com.danza_check.demo.exception.AsistenciaNotFoundException;
import com.danza_check.demo.exception.DuplicateAttendanceException;
import com.danza_check.demo.exception.SesionCerradaException;
import com.danza_check.demo.repository.AsistenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro y consulta de asistencias.
 */
@Service
@Transactional
public class AsistenciaService {

	private static final Logger log = LoggerFactory.getLogger(AsistenciaService.class);
	private static final String MENSAJE_DUPLICADO = "La asistencia ya fue registrada para esta sesión.";

	private final AsistenciaRepository asistenciaRepository;

	private final SesionService sesionService;

	private final ApplicationEventPublisher eventPublisher;

	public AsistenciaService(AsistenciaRepository asistenciaRepository, SesionService sesionService,
			ApplicationEventPublisher eventPublisher) {
		this.asistenciaRepository = asistenciaRepository;
		this.sesionService = sesionService;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Registra la asistencia de un alumno usando el codigo publico de la
	 * sesion. Valida que la sesion exista, este activa y que el correo no
	 * haya sido registrado antes en esa sesion.
	 */
	public AsistenciaResponse registrar(String codigo, RegistrarAsistenciaRequest request) {
		SesionAsistencia sesion = sesionService.buscarPorCodigo(codigo);
		if (!sesion.isActiva()) {
			throw new SesionCerradaException("La sesión ya está cerrada y no admite nuevas asistencias.");
		}
		String correo = normalizarCorreo(request.correo());
		if (asistenciaRepository.existsBySesionIdAndCorreo(sesion.getId(), correo)) {
			throw new DuplicateAttendanceException(MENSAJE_DUPLICADO);
		}
		try {
			Asistencia asistencia = new Asistencia(request.nombre().trim(), correo, LocalDateTime.now(), sesion);
			AsistenciaResponse guardada = aResponse(asistenciaRepository.saveAndFlush(asistencia));
			// Se avisa por el stream, pero el oyente espera al commit: si
			// esta transaccion revierte, el panel nunca llega a verla.
			eventPublisher.publishEvent(new AsistenciaRegistradaEvento(sesion.getId(), guardada));
			return guardada;
		}
		catch (DataIntegrityViolationException ex) {
			// Dos peticiones simultáneas con el mismo correo: la restricción
			// única de PostgreSQL rechaza la segunda.
			log.debug("Restricción única de asistencias activada en la sesión {}.", sesion.getId());
			throw new DuplicateAttendanceException(MENSAJE_DUPLICADO);
		}
	}

	@Transactional(readOnly = true)
	public List<AsistenciaResponse> listarPorSesion(Long sesionId) {
		sesionService.buscarPorId(sesionId);
		return asistenciaRepository.findBySesionIdOrderByFechaHoraAscIdAsc(sesionId).stream()
			.map(AsistenciaService::aResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public AsistenciaCountResponse contarPorSesion(Long sesionId) {
		sesionService.buscarPorId(sesionId);
		return new AsistenciaCountResponse(asistenciaRepository.countBySesionId(sesionId));
	}

	/**
	 * Borra una asistencia de la sesión. Se usa cuando alguien se apunta por
	 * error, pone un correo equivocado y el listado hay que corregirlo.
	 *
	 * <p>La búsqueda va acotada por sesión a propósito: si solo se mirara el
	 * id, un id válido de otra sesión se podría borrar desde esta URL.
	 */
	public void eliminar(Long sesionId, Long asistenciaId) {
		sesionService.buscarPorId(sesionId);
		Asistencia asistencia = asistenciaRepository.findByIdAndSesionId(asistenciaId, sesionId)
			.orElseThrow(() -> new AsistenciaNotFoundException(
					"La asistencia " + asistenciaId + " no pertenece a la sesión " + sesionId + "."));
		asistenciaRepository.delete(asistencia);
		// flush explicito: el borrado sale dentro de esta transacción y no se
		// aplaza hasta que termine, para no devolver 204 y dejar la fila viva.
		asistenciaRepository.flush();
	}

	/**
	 * Los correos se guardan en minusculas para que Juan@correo.com y
	 * juan@correo.com se consideren la misma persona.
	 */
	public static String normalizarCorreo(String correo) {
		return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
	}

	private static AsistenciaResponse aResponse(Asistencia asistencia) {
		return new AsistenciaResponse(asistencia.getId(), asistencia.getNombre(), asistencia.getCorreo(),
			asistencia.getFechaHora());
	}

}
