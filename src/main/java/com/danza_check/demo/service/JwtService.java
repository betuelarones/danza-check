package com.danza_check.demo.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.danza_check.demo.config.JwtProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

/**
 * Emite tokens JWT firmados con HS256 para el administrador.
 */
@Service
public class JwtService {

	private static final String TIPO_TOKEN = "JWT";
	private static final String CLAIM_ROL = "rol";
	private static final String ROL_ADMIN = "ADMIN";

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;

	public JwtService(JwtEncoder jwtEncoder, JwtProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	public String generarToken(String username) {
		Instant ahora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.subject(username)
			.issuedAt(ahora)
			.expiresAt(ahora.plus(properties.expirationMinutes(), ChronoUnit.MINUTES))
			.claim(CLAIM_ROL, ROL_ADMIN)
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type(TIPO_TOKEN).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	/**
	 * Vigencia del token en segundos.
	 */
	public long expiracionEnSegundos() {
		return properties.expirationMinutes() * 60;
	}

}
