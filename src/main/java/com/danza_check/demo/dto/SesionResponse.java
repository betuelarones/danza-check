package com.danza_check.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Vista de sesion para el panel administrativo.
 */
public record SesionResponse(Long id, String nombre, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
		String codigo, boolean activa, LocalDateTime createdAt) {
}
