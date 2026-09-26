package com.danza_check.demo.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.danza_check.demo.exception.ApiErrorWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

/**
 * Seguridad del backend. Solo el panel administrativo esta protegido.
 *
 * <p>Autenticacion: JWT stateless firmado con HS256. El administrador
 * recibe el token en POST /api/auth/login y lo envia en cada peticion
 * como "Authorization: Bearer {token}". No hay sesiones en servidor, por
 * lo que tambien se desactiva la proteccion CSRF.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
	private static final String ROL_ADMIN = "ADMIN";
	private static final String ALGORITMO_FIRMA = "HmacSHA256";
	private static final int LONGITUD_MINIMA_SECRET = 32;
	private static final String MENSAJE_CREDENCIALES = "Usuario o contraseña incorrectos.";

	private final ApiErrorWriter apiErrorWriter;

	public SecurityConfig(ApiErrorWriter apiErrorWriter) {
		this.apiErrorWriter = apiErrorWriter;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(requests -> requests
				.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/sesiones/codigo/*").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/sesiones/*/asistencias").permitAll()
				.requestMatchers("/error").permitAll()
				.anyRequest().authenticated())
			// El entry point tambien se fija en el resource server porque,
			// por defecto, Spring Security responde con una cabecera
			// WWW-Authenticate y sin cuerpo cuando el token no se puede
			// decodificar o ha caducado.
			.oauth2ResourceServer(oauth2 -> oauth2
				.authenticationEntryPoint(apiErrorWriter::commence)
				.jwt(Customizer.withDefaults()))
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(apiErrorWriter::commence)
				.accessDeniedHandler(apiErrorWriter::handle))
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.build();
	}

	@Bean
	UserDetailsService adminUserDetailsService(AdminProperties properties, PasswordEncoder passwordEncoder) {
		if (!StringUtils.hasText(properties.username()) || !StringUtils.hasText(properties.password())) {
			throw new IllegalStateException(
				"Faltan las credenciales del administrador. Configura ADMIN_USERNAME y ADMIN_PASSWORD.");
		}
		String username = properties.username().trim();
		String passwordHash = passwordEncoder.encode(properties.password());
		log.info("Administrador '{}' cargado desde variables de entorno.", username);
		// Se construye un User nuevo en cada consulta porque Spring Security
		// borra las credenciales del UserDetails devuelto en cuanto valida
		// una autenticacion: si se compartiera la misma instancia, el hash
		// quedaria vacio y los siguientes inicios de sesion fallarian.
		return candidate -> {
			if (!username.equals(candidate)) {
				throw new UsernameNotFoundException(MENSAJE_CREDENCIALES);
			}
			return User.withUsername(username)
				.password(passwordHash)
				.roles(ROL_ADMIN)
				.build();
		};
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	SecretKey jwtSecretKey(JwtProperties properties) {
		if (!StringUtils.hasText(properties.secret())
			|| properties.secret().getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_SECRET) {
			throw new IllegalStateException(
				"JWT_SECRET no esta configurado o es demasiado corto: se requieren al menos "
					+ LONGITUD_MINIMA_SECRET + " caracteres.");
		}
		return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), ALGORITMO_FIRMA);
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return NimbusJwtEncoder.withSecretKey(jwtSecretKey).algorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
	}

}
