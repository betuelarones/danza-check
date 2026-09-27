package com.danza_check.demo.evento;

import com.danza_check.demo.dto.AsistenciaResponse;

/**
 * Se publica cuando una asistencia queda confirmada en la base de datos.
 *
 * <p>Se emite desde el servicio dentro de la transaccion, pero se escucha
 * despues del commit: si la transaccion revierte (por ejemplo, el correo
 * ya estaba registrado en esa sesion), el panel nunca ve una asistencia
 * que en realidad no existe.
 */
public record AsistenciaRegistradaEvento(Long sesionId, AsistenciaResponse asistencia) {
}
