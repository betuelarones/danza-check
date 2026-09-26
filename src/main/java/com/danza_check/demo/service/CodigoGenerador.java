package com.danza_check.demo.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Genera codigos de sesion aleatorios de 6 caracteres.
 *
 * <p>El alfabeto excluye I, O, 0 y 1 para evitar confusiones al leer o
 * dictar un codigo. El codigo no se deriva del id de la sesion: se
 * genera al azar con SecureRandom y se valida su unicidad en la base de
 * datos.
 */
@Component
public class CodigoGenerador {

	public static final int LONGITUD_CODIGO = 6;

	private static final char[] ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

	private final SecureRandom secureRandom = new SecureRandom();

	public String generar() {
		StringBuilder codigo = new StringBuilder(LONGITUD_CODIGO);
		for (int i = 0; i < LONGITUD_CODIGO; i++) {
			codigo.append(ALFABETO[secureRandom.nextInt(ALFABETO.length)]);
		}
		return codigo.toString();
	}

}
