# DanzaCheck - Backend

API REST del sistema de control de asistencia a ensayos de danza. Permite que
la delegada cree sesiones, comparta un código (QR) con el grupo y registre la
asistencia de cada alumno desde el móvil, sin necesidad de cuenta.

- **Java 21**
- **Spring Boot 4.1.1** (Web, Validation, Data JPA, Security)
- **PostgreSQL** en producción, **H2** en las pruebas
- **JWT HS256** para autenticar al administrador del panel
- **Maven Wrapper** (`./mvnw`), no hace falta tener Maven instalado

---

## 1. Requisitos previos

| Requisito | Versión | Notas |
|---|---|---|
| JDK | 21 | `java -version` |
| PostgreSQL | 14 o superior | Solo en local y producción; las pruebas usan H2 |
| Maven | 3.9+ | Incluido vía `./mvnw` |

---

## 2. Puesta en marcha

```bash
git clone <url-del-repositorio>
cd danza-check
copy .env.example .env      # Windows
cp .env.example .env        # Linux / macOS
```

Edita `.env` con tus valores (**en Windows PowerShell**, para cargar las
variables de la sesión actual):

```powershell
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
  $nombre, $valor = $_ -split '=', 2
  [Environment]::SetEnvironmentVariable($nombre.Trim(), $valor, 'User')
}
```

Crea la base de datos en PostgreSQL:

```sql
CREATE DATABASE danzacheck;
```

Comprueba que la aplicación arranca:

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

### Variables de entorno

| Variable | Obligatoria | Valor por defecto | Descripción |
|---|---|---|---|
| `DB_URL` | Sí | `jdbc:postgresql://localhost:5432/danzacheck` | JDBC de PostgreSQL |
| `DB_USERNAME` | Sí | `danza` | Usuario de la base de datos |
| `DB_PASSWORD` | Sí | `danza` | Contraseña de la base de datos |
| `DDL_AUTO` | No | `update` | `update` en local, `validate` en producción |
| `ADMIN_USERNAME` | Sí | `admin` | Usuario del panel administrativo |
| `ADMIN_PASSWORD` | Sí | (vacío) | Contraseña del administrador |
| `JWT_SECRET` | Sí | (vacío) | Clave de firma, **mínimo 32 caracteres** |
| `JWT_ISSUER` | No | `danzacheck` | Emisor (`iss`) del token |
| `JWT_EXPIRATION_MINUTES` | No | `480` | Validez del token en minutos |
| `FRONTEND_URL` | No | `http://localhost:5173` | Origen permitido por CORS, sin barra final |
| `PORT` | No | `8080` | Puerto; Render lo inyecta automáticamente |

> No hay credenciales reales en el repositorio. `.env` está en `.gitignore`;
> solo se versiona `.env.example` con valores ficticios.

Genera un `JWT_SECRET` aleatorio:

```bash
openssl rand -base64 48
```

---

## 3. Base de datos

El esquema lo crea Hibernate (`spring.jpa.hibernate.ddl-auto`):

### `sesion_asistencia`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | `bigint` | Clave primaria autogenerada |
| `nombre` | `varchar(120)` | Obligatorio |
| `fecha` | `date` | Obligatorio |
| `hora_inicio` | `time` | Obligatorio |
| `hora_fin` | `time` | Opcional, no puede ser anterior a `hora_inicio` |
| `codigo` | `varchar(12)` | 6 caracteres, **único**, es lo que se codifica en el QR |
| `activa` | `boolean` | `false` cuando la sesión se cierra |
| `created_at` | `timestamp` | Solo lectura, se rellena al crear |

### `asistencia`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | `bigint` | Clave primaria autogenerada |
| `sesion_id` | `bigint` | FK a `sesion_asistencia`, con índice |
| `nombre` | `varchar(120)` | Obligatorio |
| `correo` | `varchar(254)` | Obligatorio, se guarda en minúsculas |
| `fecha_hora` | `timestamp` | Momento del registro |

Restricción única `uk_asistencia_sesion_correo` sobre `(sesion_id, correo)`: un
alumno no puede registrarse dos veces en la misma sesión ni siquiera si envía su
correo con distinta capitalización.

En producción se recomienda `DDL_AUTO=validate` y generar el esquema con
migraciones versionadas (Flyway o Liquibase) para que los cambios sean
reproducibles.

---

## 4. Autenticación

No hay tabla de usuarios: existe un **único administrador** (la delegada) cuyas
credenciales vienen de `ADMIN_USERNAME` y `ADMIN_PASSWORD`. La contraseña se
compara con BCrypt y **nunca** se guarda ni se registra en logs.

1. `POST /api/auth/login` con usuario y contraseña.
2. La respuesta incluye un JWT firmado con HS256, que el frontend envía en cada
   petición como `Authorization: Bearer {token}`.
3. `POST /api/auth/logout` no invalida el token: es el frontend quien lo borra.
   Los tokens siguen siendo válidos hasta que expiran (`JWT_EXPIRATION_MINUTES`).

El token incluye `sub` (usuario), `iss` (emisor), `iat`, `exp` y `rol: ADMIN`.

Las peticiones sin token válido a un endpoint protegido devuelven `401`, y un
token válido contra un endpoint no permitido devuelve `403`; ambos con el
formato de error común.

---

## 5. Endpoints

### Públicos

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| `POST` | `/api/auth/login` | Inicia sesión | `200` `LoginResponse` |
| `GET` | `/api/sesiones/codigo/{codigo}` | Datos públicos de la sesión | `200` `SesionPublicResponse` |
| `POST` | `/api/sesiones/{codigo}/asistencias` | Registra asistencia | `201` `AsistenciaResponse` |

### Protegidos (requieren `Authorization: Bearer {token}`)

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| `POST` | `/api/auth/logout` | Cierra sesión en el cliente | `204` |
| `GET` | `/api/auth/me` | Administrador autenticado | `200` `AdminResponse` |
| `POST` | `/api/sesiones` | Crea una sesión y su código | `201` `SesionResponse` |
| `GET` | `/api/sesiones` | Lista sesiones (fecha desc) | `200` `SesionResponse[]` |
| `GET` | `/api/sesiones/{id}` | Detalle de una sesión | `200` `SesionResponse` |
| `PATCH` | `/api/sesiones/{id}/cerrar` | Cierra la sesión | `200` `SesionResponse` |
| `GET` | `/api/sesiones/{id}/asistencias` | Asistencias de la sesión | `200` `AsistenciaResponse[]` |
| `GET` | `/api/sesiones/{id}/asistencias/count` | Total de asistencias | `200` `AsistenciaCountResponse` |

El código de sesión se normaliza (sin espacios y en mayúsculas), así que el
lector del QR puede devolverlo en cualquier formato. No se renueva al registrar
asistencia: el objetivo es que el QR siga siendo válido durante todo el ensayo.

---

## 6. Ejemplos de uso

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"tu-clave"}'
```

```json
{
  "token": "eyJraWQiOiJmSktY...",
  "tokenType": "Bearer",
  "expiresIn": 28800,
  "username": "admin"
}
```

### Crear una sesión

```bash
curl -X POST http://localhost:8080/api/sesiones \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Ensayo general","fecha":"2026-09-25","horaInicio":"19:00"}'
```

```json
{
  "id": 1,
  "nombre": "Ensayo general",
  "fecha": "2026-09-25",
  "horaInicio": "19:00:00",
  "horaFin": null,
  "codigo": "K7QM3Z",
  "activa": true,
  "createdAt": "2026-09-20T18:02:11.412"
}
```

### Registrar asistencia (público, desde el móvil)

```bash
curl -X POST http://localhost:8080/api/sesiones/K7QM3Z/asistencias \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Lucía Fernández","correo":"lucia@example.com"}'
```

```json
{
  "id": 12,
  "nombre": "Lucía Fernández",
  "correo": "lucia@example.com",
  "fechaHora": "2026-09-25T19:34:20.531"
}
```

### Ver la lista de asistencias de una sesión

```bash
curl http://localhost:8080/api/sesiones/1/asistencias \
  -H "Authorization: Bearer $TOKEN"
```

### Formato de fechas y horas

Jackson 3 escribe los tipos de `java.time` en ISO-8601, sin configuración
adicional:

| Tipo | Ejemplo |
|---|---|
| `LocalDate` | `2026-09-25` |
| `LocalTime` | `19:00:00` |
| `LocalDateTime` | `2026-09-25T19:34:20.531` |

Se aceptan las formas abreviadas habituales al enviar datos (`"19:00"`).

---

## 7. Formato de errores

Todos los errores usan la misma estructura, también los 401 y 403 del filtro de
seguridad:

```json
{
  "status": 409,
  "message": "La asistencia ya fue registrada para esta sesión.",
  "timestamp": "2026-09-25T19:35:02.117"
}
```

Los mensajes de validación incluyen el campo y el motivo, por ejemplo
`"nombre: es obligatorio"`, `"correo: no tiene un formato válido"` o
`"nombre: no puede superar los 120 caracteres"`.

| Código | Cuándo ocurre |
|---|---|
| `400` | Datos inválidos: campos obligatorios, formato de correo, horas incoherentes, JSON mal formado |
| `401` | Login incorrecto, token ausente, caducado o con firma inválida |
| `403` | Token válido sobre un recurso no permitido |
| `404` | Sesión o ruta inexistente |
| `409` | Asistencia duplicada en la misma sesión o sesión ya cerrada |
| `500` | Error inesperado (el detalle solo queda en los logs del servidor) |

Los filtros de seguridad se ejecutan antes que el controlador, así que una
petición **sin token** a cualquier ruta que no sea pública devuelve `401`, incluso
si la ruta no existe. Con token válido, una ruta inexistente devuelve `404` y un
método no permitido devuelve `405`.

---

## 8. Pruebas

```bash
./mvnw test              # solo pruebas
./mvnw clean verify      # compila, ejecuta las pruebas y empaqueta
```

Las pruebas usan **H2 en memoria** con el perfil `test` y no necesitan
PostgreSQL ni variables de entorno reales. Cubren autenticación, ciclo de vida
de sesiones, registro de asistencias, duplicados, sesión cerrada y validación de
entrada.

> Si el IDE compila dentro de `target/classes`, usa siempre `clean`: Maven
> reutiliza las clases si parecen actualizadas y los fallos que aparecen son
> engañosos.

---

## 9. Estructura del proyecto

```
src/
├── main/
│   ├── java/com/danza_check/demo/
│   │   ├── config/        # seguridad, CORS y propiedades tipadas
│   │   ├── controller/    # endpoints REST
│   │   ├── dto/           # records de entrada y salida
│   │   ├── entity/        # entidades JPA
│   │   ├── exception/     # excepciones y formato común de error
│   │   ├── repository/    # acceso a datos
│   │   └── service/       # lógica de negocio
│   └── resources/
│       ├── application.properties
│       └── application-dev.properties
└── test/
    ├── java/com/danza_check/demo/
    └── resources/application-test.properties
```

Convenciones:

- Controllers delgados: validan, delegan y traducen a `ResponseEntity`.
- La lógica vive en los servicios, con `@Transactional`.
- Los DTO son `record` inmutables; las entidades nunca se exponen directamente.
- Las excepciones de negocio heredan de `ApiException` y llevan su propio
  código HTTP; `GlobalExceptionHandler` las convierte en `ApiError`.

---

## 10. Despliegue

El proyecto está preparado para servicios que construyen el jar (Render,
Railway, Fly.io, Heroku) o para una máquina con Java 21:

```bash
./mvnw clean package
java -jar target/danza-check-0.0.1-SNAPSHOT.jar
```

- `server.port` ya lee `PORT`, que Render inyecta automáticamente.
- `DDL_AUTO=validate` y una base de datos gestionada (Neon, Supabase, RDS) para
  no depender de disco local.
- `FRONTEND_URL` debe apuntar al dominio real del frontend, sin barra final, o
  el navegador bloqueará las llamadas por CORS.
- El plan de Render exige nivel **pagado**: los planes gratuitos no ofrecen
  PostgreSQL persistente.
- `JWT_SECRET` debe ser una cadena aleatoria distinta en cada entorno.
# danza-check
