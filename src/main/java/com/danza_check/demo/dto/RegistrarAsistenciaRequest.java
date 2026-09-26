package com.danza_check.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarAsistenciaRequest(

		@NotBlank(message = "es obligatorio")
		@Size(max = 120, message = "no puede superar los 120 caracteres")
		String nombre,

		@NotBlank(message = "es obligatorio")
		@Email(message = "no tiene un formato válido")
		@Size(max = 254, message = "no puede superar los 254 caracteres")
		String correo) {
}
