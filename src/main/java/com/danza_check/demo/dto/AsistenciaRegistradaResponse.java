package com.danza_check.demo.dto;

/**
 * Payload del evento SSE "asistencia.registrada".
 *
 * <p>Viaja la asistencia recien creada y el conteo ya calculado, para que
 * el panel no tenga que volver a pedir la lista completa ni el conteo
 * por cada persona que se apunta.
 */
public record AsistenciaRegistradaResponse(AsistenciaResponse asistencia, long cantidad) {
}
