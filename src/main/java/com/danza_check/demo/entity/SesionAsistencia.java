package com.danza_check.demo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Sesion de ensayo. El codigo es unico y se genera en el backend; no
 * esta basado en el id de la sesion.
 */
@Entity
@Table(name = "sesion_asistencia")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SesionAsistencia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(nullable = false)
	private LocalDate fecha;

	@Column(name = "hora_inicio", nullable = false)
	private LocalTime horaInicio;

	@Column(name = "hora_fin")
	private LocalTime horaFin;

	@Column(nullable = false, unique = true, length = 12)
	private String codigo;

	@Column(nullable = false)
	private Boolean activa;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public SesionAsistencia(String nombre, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String codigo) {
		this.nombre = nombre;
		this.fecha = fecha;
		this.horaInicio = horaInicio;
		this.horaFin = horaFin;
		this.codigo = codigo;
		this.activa = Boolean.TRUE;
		this.createdAt = LocalDateTime.now();
	}

	public void cerrar() {
		this.activa = Boolean.FALSE;
	}

	public boolean isActiva() {
		return Boolean.TRUE.equals(this.activa);
	}

}
