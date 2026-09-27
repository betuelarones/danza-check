package com.danza_check.demo.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Acepta una unica variable {@code DATABASE_URL} con la forma
 * {@code postgresql://usuario:clave@host:puerto/base} y la traduce a las tres
 * propiedades que necesita el driver JDBC.
 *
 * <p>Es la convencion que ya usan Prisma, Drizzle y el resto del ecosistema:
 * en lugar de repetir URL, usuario y clave por separado, se pega el string
 * que entrega el proveedor. Se ejecuta antes de que Spring cree el
 * {@code DataSource}, asi que ahi ya esta todo resuelto.
 *
 * <p>Si viene informada {@code DB_URL} (o {@code SPRING_DATASOURCE_URL}), no
 * se toca nada: esa via sigue funcionando y tiene prioridad. Solo entra esta
 * cuando no hay nada definido a mano.
 */
public class DataSourceUrlPostProcessor implements EnvironmentPostProcessor {

	/** Variable de conexion completa, estilo Prisma. */
	private static final String DATABASE_URL = "DATABASE_URL";

	private static final String PREFIJO_JDBC_POSTGRES = "jdbc:postgresql://";
	private static final String PUERTO_POR_DEFECTO = "5432";

	private static final String PROPIEDAD_URL = "spring.datasource.url";
	private static final String PROPIEDAD_USUARIO = "spring.datasource.username";
	private static final String PROPIEDAD_CLAVE = "spring.datasource.password";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment entorno,
			SpringApplication aplicacion) {
		// Si el datasource se configuro a mano, esa via gana. Se mira
		// DB_URL y no spring.datasource.url porque esa clave siempre
		// existe en application.properties (aunque con el valor por
		// defecto) y el chequeo daria true siempre.
		if (entorno.getProperty("DB_URL") != null
				|| entorno.getProperty("SPRING_DATASOURCE_URL") != null) {
			return;
		}
		String conexion = entorno.getProperty(DATABASE_URL);
		if (conexion == null || conexion.isBlank()) {
			return;
		}

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(PROPIEDAD_URL, aJdbcUrl(conexion));
		properties.put(PROPIEDAD_USUARIO, usuario(conexion));
		// Puede quedar vacio: hay bases sin clave (por ejemplo en pruebas).
		String clave = clave(conexion);
		if (clave != null) {
			properties.put(PROPIEDAD_CLAVE, clave);
		}

		// addFirst: estas propiedades pisan las de application.properties.
		entorno.getPropertySources().addFirst(new MapPropertySource("dataSourceUrl", properties));
	}

	/** Convierte {@code postgresql://host/base} en una URL de JDBC. */
	private String aJdbcUrl(String conexion) {
		URI uri = parsear(conexion);
		String host = uri.getHost();
		if (host == null) {
			throw errorDeFormato(conexion);
		}
		StringBuilder url = new StringBuilder(PREFIJO_JDBC_POSTGRES).append(host);
		if (uri.getPort() != -1) {
			url.append(':').append(uri.getPort());
		} else {
			url.append(':').append(PUERTO_POR_DEFECTO);
		}
		String ruta = uri.getPath() == null || uri.getPath().isBlank() ? "/postgres" : uri.getPath();
		url.append(ruta);
		// Los parametros (?sslmode=require, etc.) se respetan tal cual.
		if (uri.getRawQuery() != null) {
			url.append('?').append(uri.getRawQuery());
		}
		return url.toString();
	}

	private String usuario(String conexion) {
		String info = infoDeUsuario(conexion);
		if (info == null) {
			throw new IllegalStateException(
					DATABASE_URL + " debe traer el usuario: postgresql://usuario:clave@host/base");
		}
		int separador = info.indexOf(':');
		return decodificar(separador == -1 ? info : info.substring(0, separador));
	}

	private String clave(String conexion) {
		String info = infoDeUsuario(conexion);
		if (info == null) {
			return null;
		}
		int separador = info.indexOf(':');
		if (separador == -1) {
			return null;
		}
		return decodificar(info.substring(separador + 1));
	}

	/**
	 * Parte "usuario:clave" sin pasar por los metodos ya decodificados de
	 * {@link URI}, porque aca interesa separar en el primer dos puntos: una
	 * clave puede contener ":".
	 */
	private String infoDeUsuario(String conexion) {
		URI uri = parsear(conexion);
		// Si la URI no trae usuario, se usa el del esquema
		// (postgres://db.alfaklfj -> host).
		String info = uri.getRawUserInfo();
		if (info != null && !info.isBlank()) {
			return info;
		}
		return uri.getUserInfo();
	}

	private URI parsear(String conexion) {
		try {
			return new URI(conexion.trim());
		} catch (URISyntaxException fallo) {
			throw errorDeFormato(conexion);
		}
	}

	/**
	 * Un solo mensaje para los dos casos en que la conexion no se puede
	 * interpretar, porque la causa casi siempre es la misma: un caracter
	 * especial sin codificar en la clave.
	 */
	private IllegalStateException errorDeFormato(String conexion) {
		// Lo mas comun no es un caracter sin codificar, sino una clave
		// pegada en el lugar equivocado: quedan dos "@" y el host deja
		// de existir. Conviene distinguirlo porque el arreglo es otro.
		int arrobas = contarArrobasDeLaAuthority(conexion);
		if (arrobas > 1) {
			return new IllegalStateException(DATABASE_URL + " tiene " + arrobas + " arrobas: "
					+ "la contrasena va entre los dos puntos y la arroba, y despues viene "
					+ "directamente el host. Formato: postgresql://usuario:clave@host:puerto/base. "
					+ "Valor recibido: " + conexion);
		}
		return new IllegalStateException(DATABASE_URL + " no tiene un formato valido: " + conexion
				+ ". Debe ser postgresql://usuario:clave@host:puerto/base"
				+ ". Si la clave tiene @, : o /, codificalos (%40, %3A, %2F).");
	}

	/** Cuenta las "@" de la parte que va antes de la ruta, sin contarla si es una IP. */
	private int contarArrobasDeLaAuthority(String conexion) {
		int corte = conexion.indexOf('?');
		String sinQuery = corte == -1 ? conexion : conexion.substring(0, corte);
		int ruta = sinQuery.indexOf('/', sinQuery.indexOf("//") + 2);
		String authority = ruta == -1 ? sinQuery : sinQuery.substring(0, ruta);
		int total = 0;
		for (int i = 0; i < authority.length(); i++) {
			if (authority.charAt(i) == '@') {
				total++;
			}
		}
		return total;
	}

	private String decodificar(String valor) {
		return URLDecoder.decode(valor, StandardCharsets.UTF_8);
	}
}
