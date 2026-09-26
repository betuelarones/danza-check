package com.danza_check.demo.controller;

import java.util.List;

import com.danza_check.demo.dto.AsistenciaCountResponse;
import com.danza_check.demo.dto.AsistenciaResponse;
import com.danza_check.demo.dto.RegistrarAsistenciaRequest;
import com.danza_check.demo.service.AsistenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sesiones")
public class AsistenciaController {

	private final AsistenciaService asistenciaService;

	public AsistenciaController(AsistenciaService asistenciaService) {
		this.asistenciaService = asistenciaService;
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

}
