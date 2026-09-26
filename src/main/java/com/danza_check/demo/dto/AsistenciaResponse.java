package com.danza_check.demo.dto;

import java.time.LocalDateTime;

public record AsistenciaResponse(Long id, String nombre, String correo, LocalDateTime fechaHora) {
}
