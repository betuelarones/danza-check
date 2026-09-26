package com.danza_check.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Vista publica de una sesion, accessible por codigo sin autenticacion.
 * Solo contiene lo necesario para mostrar el formulario de asistencia.
 */
public record SesionPublicResponse(String nombre, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
		boolean activa) {
}
