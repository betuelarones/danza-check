package com.danza_check.demo.service;

import java.util.List;
import java.util.Locale;

import com.danza_check.demo.dto.CrearSesionRequest;
import com.danza_check.demo.dto.SesionPublicResponse;
import com.danza_check.demo.dto.SesionResponse;
import com.danza_check.demo.entity.SesionAsistencia;
import com.danza_check.demo.exception.CodigoGeneracionException;
import com.danza_check.demo.exception.CodigoInvalidoException;
import com.danza_check.demo.exception.DatosInvalidosException;
import com.danza_check.demo.exception.SesionNotFoundException;
import com.danza_check.demo.repository.SesionAsistenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SesionService {

	private static final Logger log = LoggerFactory.getLogger(SesionService.class);
	private static final int MAX_INTENTOS_CODIGO = 10;

	private final SesionAsistenciaRepository sesionRepository;
	private final CodigoGenerador codigoGenerador;

	public SesionService(SesionAsistenciaRepository sesionRepository, CodigoGenerador codigoGenerador) {
		this.sesionRepository = sesionRepository;
		this.codigoGenerador = codigoGenerador;
	}

	public SesionResponse crear(CrearSesionRequest request) {
		if (request.horaFin() != null && request.horaFin().isBefore(request.horaInicio())) {
			throw new DatosInvalidosException("La hora de fin debe ser igual o posterior a la hora de inicio.");
		}
		SesionAsistencia sesion = new SesionAsistencia(request.nombre().trim(), request.fecha(), request.horaInicio(),
			request.horaFin(), generarCodigoUnico());
		return aResponse(sesionRepository.save(sesion));
	}

	@Transactional(readOnly = true)
	public List<SesionResponse> listar() {
		return sesionRepository.findAllByOrderByFechaDescCreatedAtDesc().stream()
			.map(SesionService::aResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public SesionResponse obtener(Long id) {
		return aResponse(buscarPorId(id));
	}

	@Transactional(readOnly = true)
	public SesionPublicResponse obtenerPorCodigo(String codigo) {
		SesionAsistencia sesion = buscarPorCodigo(codigo);
		return new SesionPublicResponse(sesion.getNombre(), sesion.getFecha(), sesion.getHoraInicio(),
			sesion.getHoraFin(), sesion.isActiva());
	}

	public SesionResponse cerrar(Long id) {
		SesionAsistencia sesion = buscarPorId(id);
		if (sesion.isActiva()) {
			sesion.cerrar();
			sesionRepository.save(sesion);
			log.info("Sesión {} ({}) cerrada.", sesion.getId(), sesion.getCodigo());
		}
		return aResponse(sesion);
	}

	@Transactional(readOnly = true)
	public SesionAsistencia buscarPorId(Long id) {
		return sesionRepository.findById(id)
			.orElseThrow(() -> new SesionNotFoundException("No existe una sesión con el id " + id + "."));
	}

	@Transactional(readOnly = true)
	public SesionAsistencia buscarPorCodigo(String codigo) {
		String codigoNormalizado = normalizarCodigo(codigo);
		return sesionRepository.findByCodigo(codigoNormalizado)
			.orElseThrow(() -> new CodigoInvalidoException(codigoNormalizado));
	}

	/**
	 * Normaliza el código capturado desde el QR: sin espacios y en
	 * mayúsculas.
	 */
	private static String normalizarCodigo(String codigo) {
		return codigo == null ? null : codigo.trim().toUpperCase(Locale.ROOT);
	}

	private String generarCodigoUnico() {
		for (int intento = 1; intento <= MAX_INTENTOS_CODIGO; intento++) {
			String codigo = codigoGenerador.generar();
			if (!sesionRepository.existsByCodigo(codigo)) {
				return codigo;
			}
		}
		log.error("No se generó un código único para la sesión tras {} intentos.", MAX_INTENTOS_CODIGO);
		throw new CodigoGeneracionException("No fue posible generar un código único para la sesión.");
	}

	private static SesionResponse aResponse(SesionAsistencia sesion) {
		return new SesionResponse(sesion.getId(), sesion.getNombre(), sesion.getFecha(), sesion.getHoraInicio(),
			sesion.getHoraFin(), sesion.getCodigo(), sesion.isActiva(), sesion.getCreatedAt());
	}

}

