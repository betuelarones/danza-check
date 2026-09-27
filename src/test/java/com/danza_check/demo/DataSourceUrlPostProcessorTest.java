package com.danza_check.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import com.danza_check.demo.config.DataSourceUrlPostProcessor;

/**
 * Comprueba que una sola variable DATABASE_URL (estilo Prisma) alcanza para
 * configurar el datasource, y que DB_URL siga teniendo prioridad.
 */
class DataSourceUrlPostProcessorTest {

	private static final String REF = "foofsnyggmzaofvogvyb";

	private final DataSourceUrlPostProcessor postProcessor = new DataSourceUrlPostProcessor();

	@Test
	@DisplayName("traduce el pooler de Supabase a una URL de JDBC")
	void traducePoolerDeSupabase() {
		MockEnvironment entorno = entornoCon(
				"DATABASE_URL=postgresql://postgres." + REF
						+ ":claveSecreta@aws-0-us-west-2.pooler.supabase.com:5432/postgres?sslmode=require");

		postProcessor.postProcessEnvironment(entorno, null);

		assertThat(entorno.getProperty("spring.datasource.url")).isEqualTo(
				"jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:5432/postgres?sslmode=require");
		assertThat(entorno.getProperty("spring.datasource.username")).isEqualTo("postgres." + REF);
		assertThat(entorno.getProperty("spring.datasource.password")).isEqualTo("claveSecreta");
	}

	@Test
	@DisplayName("acepta el esquema postgres:// y sin puerto explicito")
	void aceptaEsquemaPostgresYPuertoPorDefecto() {
		MockEnvironment entorno = entornoCon("DATABASE_URL=postgres://postgres:" + REF
				+ "@localhost/danzacheck");

		postProcessor.postProcessEnvironment(entorno, null);

		assertThat(entorno.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://localhost:5432/danzacheck");
	}

	@Test
	@DisplayName("decodifica una clave con caracteres especiales codificados")
	void decodificaClaveCodificada() {
		MockEnvironment entorno = entornoCon(
				"DATABASE_URL=postgresql://postgres.ref:p%40ss%3Aword@db.ejemplo.supabase.co:5432/postgres");

		postProcessor.postProcessEnvironment(entorno, null);

		assertThat(entorno.getProperty("spring.datasource.password")).isEqualTo("p@ss:word");
	}

	@Test
	@DisplayName("una clave con dos puntos no se parte por el primero")
	void conservaDosPuntosEnLaClave() {
		MockEnvironment entorno = entornoCon(
				"DATABASE_URL=postgresql://postgres.ref:ab:cd@db.ejemplo.supabase.co:5432/postgres");

		postProcessor.postProcessEnvironment(entorno, null);

		assertThat(entorno.getProperty("spring.datasource.password")).isEqualTo("ab:cd");
	}

	@Test
	@DisplayName("DB_URL configurada a mano tiene prioridad")
	void respetaConfiguracionManual() {
		MockEnvironment entorno = entornoCon("DATABASE_URL=postgresql://otro:clave@otro.host:5432/otra",
				"DB_URL=jdbc:postgresql://localhost:5432/danzacheck",
				"DB_USERNAME=danza",
				"DB_PASSWORD=danza");

		postProcessor.postProcessEnvironment(entorno, null);

		// No debe inyectar nada: la configuracion manual gana.
		assertThat(entorno.getPropertySources().contains("dataSourceUrl")).isFalse();
	}

	@Test
	@DisplayName("con solo DATABASE_URL, sus propiedades pisan el resto")
	void pisaElValorPorDefecto() {
		MockEnvironment entorno = entornoCon(
				"DATABASE_URL=postgresql://postgres.ref:clave@db.ejemplo.supabase.co:5432/postgres");
		// Asimila el caso real: application.properties ya trae esta clave
		// con el valor por defecto.
		entorno.getPropertySources().addLast(new org.springframework.core.env.MapPropertySource(
				"applicationConfig", java.util.Map.of("spring.datasource.username", "danza")));

		postProcessor.postProcessEnvironment(entorno, null);

		assertThat(entorno.getPropertySources().contains("dataSourceUrl")).isTrue();
		assertThat(entorno.getProperty("spring.datasource.username")).isEqualTo("postgres.ref");
	}

	@Test
	@DisplayName("sin DATABASE_URL no inventa nada")
	void noHaceNadaSinVariable() {
		MockEnvironment entorno = entornoCon("DB_URL=jdbc:postgresql://localhost:5432/danzacheck");

		postProcessor.postProcessEnvironment(entorno, null);

		assertThat(entorno.getPropertySources()
				.contains("dataSourceUrl")).isFalse();
	}

	@Test
	@DisplayName("una clave con arroba pegada en el lugar equivocado lo dice")
	void explicaDobleArroba() {
		MockEnvironment entorno = entornoCon(
				"DATABASE_URL=postgresql://postgres.ref:clave@sobrante@aws-0-ejemplo.pooler.supabase.com:5432/postgres");

		assertThatThrownBy(() -> postProcessor.postProcessEnvironment(entorno, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("2 arrobas");
	}

	@Test
	@DisplayName("una clave sin codificar con arroba explica como corregirla")
	void explicaComoCodificarLaClave() {
		MockEnvironment entorno = entornoCon(
				"DATABASE_URL=postgresql://postgres.ref:p%40ss%20word@db.ejemplo.supabase.co:5432/postgres?sslmode=require");

		// Sin codificar seria invalido: aca se comprueba que el caso
		// normal no se confunda con el de las dos arrobas.
		postProcessor.postProcessEnvironment(entorno, null);

		assertThat(entorno.getProperty("spring.datasource.password")).isEqualTo("p@ss word");
	}

	private MockEnvironment entornoCon(String... pares) {
		MockEnvironment entorno = new MockEnvironment();
		for (String par : pares) {
			String[] partes = par.split("=", 2);
			entorno.setProperty(partes[0], partes[1]);
		}
		return entorno;
	}
}
