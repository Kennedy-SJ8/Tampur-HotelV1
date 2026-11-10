# Conexión a Supabase (PostgreSQL) - CONFIGURACIÓN REAL

Estado: **ACTIVO** · Región: `us-west-2` (Oregon)

## Resumen de la conexión

| Parámetro | Valor |
|---|---|
| Proyecto | `ogzyecgnznbhqfvwizbo` |
| Host (pooler) | `aws-0-us-west-2.pooler.supabase.com` |
| Puerto | `6543` (transaction pooler) |
| Usuario | `postgres.ogzyecgnznbhqfvwizbo` |
| Base de datos | `postgres` |
| SSL | `require` |

## Por qué usamos el pooler y no la conexión directa

La conexión directa (`db.ogzyecgnznbhqfvwizbo.supabase.co:5432`) **solo resuelve
a IPv6**. Render (y la mayoría de plataformas cloud) no soporta IPv6 de salida,
por lo que la conexión directa falla.

La solución es usar el **connection pooler** de Supabase (IPv4):

- **Transaction pooler** (puerto `6543`): límite alto de conexiones, ideal para
  Hibernate. Requiere el parámetro `pgbouncer=true` en la URL JDBC.
- **Session pooler** (puerto `5432`): límite de 15 clientes (se satura rápido con
  varios servicios).

Usamos el **transaction pooler** con `pgbouncer=true`.

## Variables de entorno en Render

Configuradas en los servicios `reservas-service` y `pagos-service`:

```
SUPABASE_DB_URL      = jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:6543/postgres?sslmode=require&pgbouncer=true
SUPABASE_DB_USER     = postgres.ogzyecgnznbhqfvwizbo
SUPABASE_DB_PASSWORD = ********
```

## Estructura de tablas

`reservas-service` y `pagos-service` usan Spring Data JPA con `ddl-auto=update`,
por lo que Hibernate crea las tablas automáticamente al arrancar. Además,
`DataSeeder` inserta las 6 habitaciones iniciales si la tabla está vacía.

Tablas en el esquema `public`:

- `habitaciones` (id, tipo, precio_noche, estado)
- `reservas` (codigo, tipo_habitacion, nombre, dni, correo, telefono,
  fecha_entrada, fecha_salida, noches, total, estado, creado_en)
- `pagos` (id, codigo_reserva, metodo, estado, voucher, creado_en)

## Cómo cambiar la contraseña o recrear

1. En Supabase Dashboard → Project Settings → Database, regenera la contraseña.
2. Actualiza la env var `SUPABASE_DB_PASSWORD` en Render.
3. Redeploy.

## Notas

- `spring.datasource.hikari.maximum-pool-size=5` (reducido a propósito para no
  saturar el pooler).
- El `creado_en` es `timestamptz` con default `now()`.
- Si el pooler da error `EMAXCONNSESSION`, es que se superó el límite de 15
  clientes del session pooler (5432). Usa el transaction pooler (6543).
