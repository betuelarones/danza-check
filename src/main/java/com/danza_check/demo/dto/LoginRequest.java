package com.danza_check.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

		@NotBlank(message = "es obligatorio")
		String username,

		@NotBlank(message = "es obligatorio")
		String password) {
}
