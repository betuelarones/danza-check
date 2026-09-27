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

Edita `.env` con tus valores. Después, un solo comando carga el archivo en la
sesión actual y arranca la API (en `http://localhost:8080`):

```powershell
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object { $n,$v = $_ -split '=',2; Set-Item "Env:$($n.Trim())" $v.Trim() }; .\mvnw.cmd spring-boot:run
```

Usa `Set-Item "Env:..."` y no `[Environment]::SetEnvironmentVariable(...,
'User')`: lo segundo escribe las variables de forma permanente en tu usuario y
no las ve el proceso que arranques en esa misma consola. El comando de arriba
solo las define en la terminal actual, así que al cerrarla desaparece todo.

Si Maven no puede descargar dependencias, agregá `-o`:
`.\mvnw.cmd -o spring-boot:run`.

<details>
<summary>Alternativa: usar un PostgreSQL local en vez de Supabase</summary>

Comentá las tres líneas de Supabase en `.env` y descomentá las del bloque
local. La base se crea una sola vez:

```sql
CREATE DATABASE danzacheck;
```

Con `DDL_AUTO=update` Hibernate crea el esquema solo al arrancar.

</details>


### Variables de entorno

| Variable | Obligatoria | Valor por defecto | Descripción |
|---|---|---|---|
| `DATABASE_URL` | Sí | — | Conexión completa estilo Prisma: `postgresql://usuario:clave@host:puerto/base` |
| `DB_URL` | No | `jdbc:postgresql://localhost:5432/danzacheck` | JDBC de PostgreSQL; tiene prioridad sobre `DATABASE_URL` |
| `DB_USERNAME` | No | `danza` | Usuario, si se prefiere separar de `DATABASE_URL` |
| `DB_PASSWORD` | No | `danza` | Clave, si se prefiere separar de `DATABASE_URL` |
| `DDL_AUTO` | No | `update` | `update` en local, `validate` en producción |
| `ADMIN_USERNAME` | Sí | `admin` | Usuario del panel administrativo |
| `ADMIN_PASSWORD` | Sí | (vacío) | Contraseña del administrador |
| `JWT_SECRET` | Sí | (vacío) | Clave de firma, **mínimo 32 caracteres** |
| `JWT_ISSUER` | No | `danzacheck` | Emisor (`iss`) del token |
| `JWT_EXPIRATION_MINUTES` | No | `480` | Validez del token en minutos |
| `FRONTEND_URL` | No | `http://localhost:5173` | Origen permitido por CORS, sin barra final |
| `PORT` | No | `8080` | Puerto; Render lo inyecta automáticamente |
| `STREAM_HEARTBEAT_MS` | No | `25000` | Cada cuánto se envía un latido en el stream |
| `STREAM_TIMEOUT_MS` | No | `0` | Vida máxima de una conexión SSE; `0` = sin límite |

> No hay credenciales reales en el repositorio. `.env` está en `.gitignore`;
> solo se versiona `.env.example` con valores ficticios.

Sobre la base de datos: con `DATABASE_URL` alcanza, es la convención de Prisma y
Drizzle y hay que pegar el string que entrega Supabase tal cual. Si la clave
tiene `@`, `:` o `/`, hay que codificarlos (`%40`, `%3A`, `%2F`); si te olvidás,
la app lo dice al arrancar. Las tres variables `DB_*` siguen existiendo para
quien las prefiera separadas, y ganan si están presentes.


Genera un `JWT_SECRET` aleatorio:

```bash
openssl rand -base64 48
```

---

## 3. Base de datos

PostgreSQL. En desarrollo puede ser un PostgreSQL local; en producción, un
proyecto de Supabase (ver [Supabase](#31-supabase-en-produccion)).

En desarrollo el esquema lo crea Hibernate al arrancar
(`spring.jpa.hibernate.ddl-auto=update`). **En producción no**: ahí
`DDL_AUTO=validate` solo comprueba que el esquema coincida con las entidades
y falla si algo falta, para que nadie modifique la base sin que quede
registrado. El esquema se aplica a mano una sola vez, con los scripts de
[`db/`](db), en el SQL Editor de Supabase:

1. `db/01-schema.sql` — tablas, índices y restricciones.
2. `db/02-row-level-security.sql` — activa RLS (ver más abajo).

A partir de ahí el esquema de `db/` es el que manda. Un cambio de entidad
que afecte al esquema se escribe primero en SQL, se aplica y recién después
se toca la entidad.

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
correo con distinta capitalización. Es la última línea de defensa: el servicio
también valida, pero si dos peticiones simultáneas pasan esa validación, la
restricción es la que rechaza la segunda.

### 3.1 Supabase en producción

Supabase se usa **solo como PostgreSQL gestionado**. No se usa su Auth, ni su
API de datos, ni Realtime: el backend sigue siendo el único que lee y escribe,
con su propio JWT.

#### Por qué el pooler y no la conexión directa

La conexión directa de Supabase (`db.<ref>.supabase.co:5432`) **solo resuelve a
IPv6**, y Render no tiene IPv6. Con la conexión directa el backend no arranca en
Render: falla al abrir la conexión.

Hay que usar el **Supavisor en modo sesión** (puerto `5432`, IPv4). Además de
resolver el tema de la red, ese modo soporta `SET` y *prepared statements*, que
Hibernate necesita. El modo transacción (puerto `6543`) sería para serverless y
no sirve aquí.

En *Project Settings → Database → Connection string*, activá **Session pooler**
y usá esos valores:

| Variable | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DB_USERNAME` | `postgres.<project-ref>` — con el sufijo del proyecto |
| `DB_PASSWORD` | La contraseña del proyecto |
| `DDL_AUTO` | `validate` |

Ojo con el usuario: en el pooler lleva `postgres.<project-ref>`, no `postgres`
a secas.

#### RLS

Cada proyecto de Supabase expone una Data API en
`https://<ref>.supabase.co/rest/v1` usando la clave pública del proyecto, que
viaja dentro del bundle del navegador y por lo tanto **es pública**. Con RLS
desactivado, esa clave alcanzaría para leer y escribir estas tablas sin pasar
por el backend: se podrían registrar asistencias falsas en una sesión cerrada o
leer la lista de correos de todo el grupo.

Por eso `db/02-row-level-security.sql` activa RLS y además revoca los permisos de
`anon` y `authenticated`. Sin policies, la Data API no devuelve nada.

Esto no afecta al backend: conecta con el rol `postgres`, que es el dueño de las
tablas y por definición ignora RLS.

**Compruébalo después de aplicar el script**, no des por hecho que quedó:

```sql
SELECT relname, relrowsecurity
  FROM pg_class
 WHERE relname IN ('asistencia', 'sesion_asistencia');
```

`relrowsecurity` debe salir en `true` en las dos. La segunda prueba es desde
fuera, con la clave pública del proyecto (que es pública, va en el bundle del
navegador):

```bash
curl "https://<ref>.supabase.co/rest/v1/asistencia?select=nombre,correo" \
  -H "apikey: <clave-publica>" \
  -H "Authorization: Bearer <clave-publica>"
```

La respuesta debe ser un error de permisos o una lista vacía. Si te devuelve
filas, RLS no está activo: para todo lo demás.

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
| `GET` | `/api/sesiones/{id}/asistencias/stream` | Stream SSE de la sesión | `200` `text/event-stream` |


El código de sesión se normaliza (sin espacios y en mayúsculas), así que el
lector del QR puede devolverlo en cualquier formato. No se renueva al registrar
asistencia: el objetivo es que el QR siga siendo válido durante todo el ensayo.

### Stream en vivo (SSE)

`GET /api/sesiones/{id}/asistencias/stream` mantiene la conexión abierta y avisa
al panel de cada cambio, para que el profesor vea las asistencias marcándose
sin recargar la página.

Eventos emitidos:

| Evento | Payload | Cuándo |
|---|---|---|
| `conectado` | `{ "cantidad": 3 }` | Al abrir la conexión, con el conteo actual |
| `asistencia.registrada` | `{ "asistencia": {...}, "cantidad": 4 }` | Cada vez que alguien se apunta |
| `sesion.cerrada` | La sesión actualizada | Al cerrarla; después el servidor cierra la conexión |

Además se envían comentarios `:latido` cada `STREAM_HEARTBEAT_MS` (25 s por
omisión). El cliente los ignora: existen para que Render y los proxies no corten
la conexión por inactividad.

Detalles que explican por qué está implementado así:

- **Se notifica después del commit**, no durante la transacción. Si se hiciera
  durante, el panel vería una asistencia que la base de datos puede rechazar
  después (por ejemplo cuando dos peticiones simultáneas traen el mismo correo):
  aparecería en pantalla y luego desaparecería al revertir.
- **Es SSE y no WebSocket** porque el flujo es unidireccional —el backend avisa,
  el panel escucha—, y SSE no necesita dependencias extra ni configuración de
  proxy. Tampoco se usa Supabase Realtime: el backend ya sabe exactamente
  cuándo hay un cambio, no hace falta suscribirse a cambios de la base.
- **El cliente no puede usar `EventSource`**, porque esa API del navegador no
  permite mandar la cabecera `Authorization`. Tiene que usar `fetch()` con un
  `ReadableStream`, que sí la manda. Ver `suscribirStreamAsistencias` en el
  frontend.

Para probarlo con `curl`:

```bash
curl -N http://localhost:8080/api/sesiones/1/asistencias/stream \
  -H "Authorization: Bearer <token>"
```


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
entrada. Del stream se comprueba que exige token, que responde `404` ante una
sesión inexistente, y sobre todo que **una asistencia rechazada no genera
notificación**: es el detalle que separa notificar después del commit de
notificar durante la transacción.


> Si el IDE compila dentro de `target/classes`, usa siempre `clean`: Maven
> reutiliza las clases si parecen actualizadas y los fallos que aparecen son
> engañosos.

---

## 9. Estructura del proyecto

```
db/
├── 01-schema.sql              # esquema: se aplica una vez en Supabase
└── 02-row-level-security.sql  # RLS: cierra la Data API pública
src/
├── main/
│   ├── java/com/danza_check/demo/
│   │   ├── config/        # seguridad, CORS y propiedades tipadas
│   │   ├── controller/    # endpoints REST y el stream SSE
│   │   ├── dto/           # records de entrada y salida
│   │   ├── entity/        # entidades JPA
│   │   ├── evento/        # eventos internos que alimenta el stream
│   │   ├── exception/     # excepciones y formato común de error
│   │   ├── repository/    # acceso a datos
│   │   └── service/       # lógica de negocio y conexiones SSE
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
- Lo que va al panel se publica como evento y se escucha en `AFTER_COMMIT`.

---

## 10. Despliegue

El proyecto está preparado para servicios que construyen el jar (Render,
Railway, Fly.io, Heroku) o para una máquina con Java 21:

```bash
./mvnw clean package
java -jar target/danza-check-0.0.1-SNAPSHOT.jar
```

- `server.port` ya lee `PORT`, que Render inyecta automáticamente.
- `FRONTEND_URL` debe apuntar al dominio real del frontend, sin barra final, o
  el navegador bloqueará las llamadas por CORS.
- `JWT_SECRET` debe ser una cadena aleatoria distinta en cada entorno.
- El plan de Render exige nivel **pagado**: los planes gratuitos no ofrecen
  PostgreSQL persistente.

Con **Supabase** como base de datos (la opción que usa este proyecto), hay que
tener en cuenta lo de la [sección 3.1](#31-supabase-en-produccion): el esquema se
aplica a mano desde `db/`, y la conexión va por el **pooler en modo sesión**,
porque Render no tiene IPv6 y la conexión directa de Supabase sí lo necesita.

Variables que hay que definir en el panel de Render:

| Variable | Valor |
|---|---|
| `DB_URL` | Pooler modo sesión, con `?sslmode=require` |
| `DB_USERNAME` | `postgres.<project-ref>` |
| `DB_PASSWORD` | Contraseña del proyecto de Supabase |
| `DDL_AUTO` | `validate` |
| `JWT_SECRET` | Aleatorio, distinto del de desarrollo |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Credenciales del panel |
| `FRONTEND_URL` | Dominio del frontend en Vercel, sin barra final |

Sobre el stream SSE y Render: las conexiones largas sobreviven mientras haya
tráfico, y el latido de 25 s lo garantiza. Si el plan de Render tiene un tiempo
máximo de conexión, bajá `STREAM_HEARTBEAT_MS`; si cortara la conexión, el
frontend reconecta solo.

# danza-check
