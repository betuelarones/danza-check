package com.danza_check.demo.controller;

import java.util.List;

import com.danza_check.demo.dto.CrearSesionRequest;
import com.danza_check.demo.dto.SesionPublicResponse;
import com.danza_check.demo.dto.SesionResponse;
import com.danza_check.demo.service.SesionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sesiones")
public class SesionController {

	private final SesionService sesionService;

	public SesionController(SesionService sesionService) {
		this.sesionService = sesionService;
	}

	@PostMapping
	public ResponseEntity<SesionResponse> crear(@Valid @RequestBody CrearSesionRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(sesionService.crear(request));
	}

	@GetMapping
	public List<SesionResponse> listar() {
		return sesionService.listar();
	}

	@GetMapping("/{id}")
	public SesionResponse obtener(@PathVariable Long id) {
		return sesionService.obtener(id);
	}

	/**
	 * Endpoint público: el alumno escanea el QR y solo necesita ver la
	 * información mínima de la sesión para completar el formulario.
	 */
	@GetMapping("/codigo/{codigo}")
	public SesionPublicResponse obtenerPorCodigo(@PathVariable String codigo) {
		return sesionService.obtenerPorCodigo(codigo);
	}

	@PatchMapping("/{id}/cerrar")
	public SesionResponse cerrar(@PathVariable Long id) {
		return sesionService.cerrar(id);
	}

	/**
	 * Elimina la sesión de forma definitiva, junto con sus asistencias. Es
	 * distinto de cerrar: cerrar conserva el historial y solo deja de admitir
	 * gente nueva.
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		sesionService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

}
