package com.danza_check.demo.evento;

import com.danza_check.demo.dto.SesionResponse;

/**
 * Se publica cuando el administrador cierra una sesion.
 *
 * <p>Lleva la sesion ya actualizada (activa en false) para que el panel
 * pueda refrescar cabecera y estado de una sola vez, y para que deje de
 * escuchar el stream.
 */
public record SesionCerradaEvento(Long sesionId, SesionResponse sesion) {
}
