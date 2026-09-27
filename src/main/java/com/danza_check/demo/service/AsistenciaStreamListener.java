package com.danza_check.demo.service;

import com.danza_check.demo.dto.AsistenciaRegistradaResponse;
import com.danza_check.demo.evento.AsistenciaRegistradaEvento;
import com.danza_check.demo.evento.SesionCerradaEvento;
import com.danza_check.demo.repository.AsistenciaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Traduce los eventos internos a mensajes del stream.
 *
 * <p>Se escucha en AFTER_COMMIT y no durante la transaccion, por dos
 * razones que importan en este dominio:
 *
 * <ul>
 * <li><b>Correccion.</b> Registrar una asistencia puede fallar al final:
 * si dos peticiones simultaneas traen el mismo correo, la restriccion
 * unica de PostgreSQL rechaza la segunda. Notificando durante la
 * transaccion, el panel veria una asistencia duplicada que despues
 * desaparece al revertir la transaccion.</li>
 * <li><b>Bloqueo.</b> El envio SSE es bloqueante. Hacerlo dentro de la
 * transaccion mantiene abierta una conexion a la base de datos mientras
 * se espera al cliente, y eso agota el pool justo cuando hay mas
 * carga.</li>
 * </ul>
 *
 * <p>Por el mismo motivo el conteo se recalcula aqui: ya no hay
 * transaccion abierta y la consulta abre la suya propia.
 */
@Component
public class AsistenciaStreamListener {

	private final AsistenciaStreamService streamService;

	private final AsistenciaRepository asistenciaRepository;

	public AsistenciaStreamListener(AsistenciaStreamService streamService,
			AsistenciaRepository asistenciaRepository) {
		this.streamService = streamService;
		this.asistenciaRepository = asistenciaRepository;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void alRegistrarAsistencia(AsistenciaRegistradaEvento evento) {
		long cantidad = asistenciaRepository.countBySesionId(evento.sesionId());
		streamService.emitir(evento.sesionId(), "asistencia.registrada",
				new AsistenciaRegistradaResponse(evento.asistencia(), cantidad));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void alCerrarSesion(SesionCerradaEvento evento) {
		streamService.emitir(evento.sesionId(), "sesion.cerrada", evento.sesion());
		// Tras avisar que se cerro la conexion ya no tiene sentido: el
		// frontend la cierra y se lleva por delante el emitter del servidor.
		streamService.cerrar(evento.sesionId());
	}

}
