package com.danza_check.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Asistencia de un alumno a una sesion.
 *
 * <p>La restriccion unica (sesion_id, correo) es la ultima linea de
 * defensa contra asistencias duplicadas: ademas de la validacion de
 * negocio, la base de datos rechaza el segundo INSERT con el mismo
 * correo en la misma sesion. El correo se normaliza a minusculas antes
 * de persistir para que no existan duplicados por mayusculas.
 */
@Entity
@Table(name = "asistencia",
		indexes = @Index(name = "idx_asistencia_sesion", columnList = "sesion_id"),
		uniqueConstraints = @UniqueConstraint(name = "uk_asistencia_sesion_correo",
			columnNames = { "sesion_id", "correo" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asistencia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(nullable = false, length = 254)
	private String correo;

	@Column(name = "fecha_hora", nullable = false)
	private LocalDateTime fechaHora;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sesion_id", nullable = false, foreignKey = @ForeignKey(name = "fk_asistencia_sesion"))
	private SesionAsistencia sesion;

	public Asistencia(String nombre, String correo, LocalDateTime fechaHora, SesionAsistencia sesion) {
		this.nombre = nombre;
		this.correo = correo;
		this.fechaHora = fechaHora;
		this.sesion = sesion;
	}

}
