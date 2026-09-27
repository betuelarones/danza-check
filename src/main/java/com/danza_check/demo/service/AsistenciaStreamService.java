package com.danza_check.demo.service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.danza_check.demo.config.StreamProperties;
import com.danza_check.demo.dto.StreamConectadoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Mantiene las conexiones SSE abiertas del panel y reparte los cambios.
 *
 * <p>El stream es unidireccional: el backend avisa, el panel escucha.
 * Por eso alcanza con SseEmitter, sin WebSocket ni dependencias extra.
 *
 * <p>Un panel abierto no es una sesion HTTP normal: es una peticion que
 * no termina. Eso obliga a cuidar tres cosas:
 * <ul>
 * <li>el emitter se desregistra en cuanto el cliente se va, para no
 * acumular conexiones muertas en memoria;</li>
 * <li>cada envio se sincroniza sobre el emitter, porque el heartbeat y
 * una asistencia pueden entrar al mismo tiempo y el stream no tolera
 * escrituras cruzadas;</li>
 * <li>se manda un latido periodico, porque Render y los proxies cierran
 * las conexiones sin trafico.</li>
 * </ul>
 */
@Service
public class AsistenciaStreamService {

	private static final Logger log = LoggerFactory.getLogger(AsistenciaStreamService.class);

	private final Map<Long, Set<SseEmitter>> suscriptores = new ConcurrentHashMap<>();

	private final StreamProperties properties;

	public AsistenciaStreamService(StreamProperties properties) {
		this.properties = properties;
	}

	/**
	 * Registra un panel como suscriptor de una sesion. El panel recibe de
	 * inmediato el conteo actual para pintar la tabla sin esperar el
	 * primer cambio.
	 */
	public SseEmitter suscribir(Long sesionId, long cantidadInicial) {
		SseEmitter emitter = new SseEmitter(properties.timeoutMs());
		Set<SseEmitter> delSesion = suscriptores.computeIfAbsent(sesionId,
				clave -> ConcurrentHashMap.newKeySet());
		delSesion.add(emitter);

		Runnable baja = () -> desuscribir(sesionId, emitter);
		emitter.onCompletion(baja);
		emitter.onTimeout(baja);
		emitter.onError(fallo -> baja.run());

		enviar(emitter, sesionId, "conectado", new StreamConectadoResponse(cantidadInicial));
		log.debug("Panel conectado al stream de la sesión {} ({} suscriptores).", sesionId, delSesion.size());
		return emitter;
	}

	/**
	 * Envia un evento a todos los paneles de una sesion. Los que fallan se
	 * cierran y se descartan: seguir guardando emitters muertos solo
	 * desperdicia memoria y loguea errores sin parar.
	 */
	public void emitir(Long sesionId, String evento, Object payload) {
		Set<SseEmitter> delSesion = suscriptores.get(sesionId);
		if (delSesion == null || delSesion.isEmpty()) {
			return;
		}
		// Se copia porque desuscribir() modifica el conjunto mientras se
		// emite, y eso romperia la iteracion.
		for (SseEmitter emitter : Set.copyOf(delSesion)) {
			enviar(emitter, sesionId, evento, payload);
		}
	}

	/**
	 * Cierra todas las conexiones de una sesion. Se usa cuando la sesion se
	 * cierra, para que el panel deje de escuchar.
	 */
	public void cerrar(Long sesionId) {
		Set<SseEmitter> delSesion = suscriptores.remove(sesionId);
		if (delSesion == null) {
			return;
		}
		for (SseEmitter emitter : delSesion) {
			complete(emitter, sesionId);
		}
	}

	/**
	 * Latido periodico. Sin trafico, un proxy intermedio (y Render)
	 * pueden cortar la conexion; un comentario SSE la mantiene viva y
	 * ademas sirve al cliente para detectar que la linea sigue viva.
	 */
	@Scheduled(fixedRateString = "${app.stream.heartbeat-ms:25000}")
	public void latido() {
		if (suscriptores.isEmpty()) {
			return;
		}
		// Copia de claves: cerrar() puede vaciar el mapa mientras corre.
		for (Map.Entry<Long, Set<SseEmitter>> entrada : Set.copyOf(suscriptores.entrySet())) {
			for (SseEmitter emitter : Set.copyOf(entrada.getValue())) {
				enviarLatido(emitter, entrada.getKey());
			}
		}
	}

	private void enviar(SseEmitter emitter, Long sesionId, String evento, Object payload) {
		try {
			// El send escribe sobre la respuesta asincrona: dos hilos a la
			// vez (un heartbeat y una asistencia) la corrompen.
			synchronized (emitter) {
				emitter.send(SseEmitter.event().name(evento).data(payload));
			}
		}
		catch (IOException | IllegalStateException ex) {
			log.debug("Se pierde la conexión al stream de la sesión {}: {}", sesionId, ex.getMessage());
			desuscribir(sesionId, emitter);
			complete(emitter, sesionId);
		}
	}

	private void enviarLatido(SseEmitter emitter, Long sesionId) {
		try {
			synchronized (emitter) {
				emitter.send(SseEmitter.event().comment("latido"));
			}
		}
		catch (IOException | IllegalStateException ex) {
			desuscribir(sesionId, emitter);
		}
	}

	private void complete(SseEmitter emitter, Long sesionId) {
		try {
			emitter.complete();
		}
		catch (RuntimeException ex) {
			log.trace("El emitter de la sesión {} ya estaba cerrado.", sesionId);
		}
	}

	private void desuscribir(Long sesionId, SseEmitter emitter) {
		Set<SseEmitter> delSesion = suscriptores.get(sesionId);
		if (delSesion == null) {
			return;
		}
		delSesion.remove(emitter);
		if (delSesion.isEmpty()) {
			// Sin el remove, el mapa acumula entradas de sesiones que ya
			// no tienen ningun panel escuchando.
			suscriptores.remove(sesionId, delSesion);
		}
	}

}
