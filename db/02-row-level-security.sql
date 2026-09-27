-- =====================================================================
-- DanzaCheck - seguridad a nivel de fila (RLS)
--
-- Por que hace falta aunque el frontend no use Supabase:
-- cada proyecto de Supabase expone la Data API (PostgREST) en
-- https://<ref>.supabase.co/rest/v1 con la clave publica del proyecto.
-- Esa clave viaja dentro del bundle del navegador, o sea que es
-- publica: cualquiera que abra las DevTools la tiene.
-- Si RLS esta desactivado, esa clave alcanza para leer y escribir estas
-- tablas saltandose el JWT del backend y toda la validacion de negocio
-- (por ejemplo, registrar asistencias falsas ni al abrir sesion).
--
-- Con RLS habilitado y sin policies, la Data API no devuelve nada.
-- El backend no se ve afectado: conecta con el rol postgres, que es
-- dueno de las tablas y por definicion ignora RLS.
--
-- Aplicar despues de 01-schema.sql.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- Sin policies a proposito: ninguna fila es legible ni escribible
-- desde la Data API. Si alguna vez hace falta dar acceso a un cliente
-- distinto del backend (por ejemplo Supabase Realtime), se agrega una
-- policy explicita para ese rol, nunca se deja abierto.
-- ---------------------------------------------------------------------

ALTER TABLE sesion_asistencia ENABLE ROW LEVEL SECURITY;
ALTER TABLE asistencia ENABLE ROW LEVEL SECURITY;

-- El dueno (postgres) y service_role ya ignoran RLS. Ahi se apoya el
-- backend, asi que no se usa FORCE ROW LEVEL SECURITY: eso extendia RLS
-- al dueno y dejaria al backend sin poder escribir.

-- Defensa adicional: quitar el privilegio en vez de confiar solo en RLS.
-- El backend no depende de estos roles porque entra como postgres.
REVOKE ALL ON TABLE sesion_asistencia FROM anon, authenticated;
REVOKE ALL ON TABLE asistencia FROM anon, authenticated;

COMMIT;

-- ---------------------------------------------------------------------
-- Comprobacion opcional: estas consultas deben devolver "Row Security
-- Enabled" en las dos tablas. Si aparece "Disabled", el paso anterior no
-- llego a aplicarse.
-- ---------------------------------------------------------------------
-- SELECT relname, relrowsecurity
--   FROM pg_class
--  WHERE relname IN ('asistencia', 'sesion_asistencia');

-- =====================================================================
-- Nota: por que el backend conecta con el rol postgres y no con uno
-- dedicado.
--
-- Un rol propio (por ejemplo danzacheck_app) es lo ideal cuando varios
-- clientes comparten la base, porque RLS se le aplica igual y el
-- permiso queda atado al rol. Aqui no aporta: el backend es el unico
-- cliente, ya valida cada regla en la capa de servicio, y usar el dueno
-- evita tener que mantener policies que solo lo dejarian pasar.
-- Si algun dia entra un segundo cliente, esa es la primera razon para
-- migrar a un rol dedicado con policies por rol.
-- =====================================================================
