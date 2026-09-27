package com.danza_check.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ajustes del stream de asistencias (SSE).
 *
 * @param heartbeatMs cada cuanto se envia un latido a los suscriptores
 * para que la conexion no se considere inactiva. Render y los proxies
 * cierran las conexiones sin trafico.
 * @param timeoutMs tiempo maximo de vida de una conexion. Con 0 no hay
 * limite y la reconexion queda a cargo del cliente.
 */
@ConfigurationProperties(prefix = "app.stream")
public record StreamProperties(long heartbeatMs, long timeoutMs) {
}
