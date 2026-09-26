package com.danza_check.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearSesionRequest(

		@NotBlank(message = "es obligatorio")
		@Size(max = 120, message = "no puede superar los 120 caracteres")
		String nombre,

		@NotNull(message = "es obligatoria")
		LocalDate fecha,

		@NotNull(message = "es obligatoria")
		LocalTime horaInicio,

		LocalTime horaFin) {
}
