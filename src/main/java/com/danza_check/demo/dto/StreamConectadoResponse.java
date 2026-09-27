package com.danza_check.demo.dto;

/**
 * Payload del evento SSE "conectado", el primero que recibe el panel al
 * abrir el stream. Lleva el conteo para que la tabla se pinte de una vez,
 * sin esperar a que alguien se apunte.
 */
public record StreamConectadoResponse(long cantidad) {
}
