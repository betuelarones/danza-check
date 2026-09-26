package com.danza_check.demo.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS limitado al origen del frontend React configurado con FRONTEND_URL.
 * Varios origenes pueden indicarse separados por coma.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private final String[] origins;

	public CorsConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
		this.origins = Arrays.stream(allowedOrigins.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isEmpty())
			.toArray(String[]::new);
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
			.allowedOrigins(origins)
			.allowedMethods("GET", "POST", "PATCH", "OPTIONS")
			.allowedHeaders("*")
			.maxAge(3600);
	}

}
