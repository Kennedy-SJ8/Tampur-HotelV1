# Conectar a Supabase (PostgreSQL)

Guía paso a paso para conectar el backend de Hotel Támpur a una base de datos
Supabase (PostgreSQL).

## 1. Crear el proyecto en Supabase

1. Entra a [https://supabase.com](https://supabase.com) y regístrate / inicia sesión.
2. Haz clic en **New project**.
   - **Name:** `hotel-tampur`
   - **Database password:** elige una contraseña segura (la necesitarás después).
   - **Region:** la más cercana (por ejemplo `South America (São Paulo)` o `US East`).
   - **Plan:** Free es suficiente para la prueba.
3. Espera ~1-2 minutos mientras se aprovisiona.

## 2. Obtener la cadena de conexión

1. En el panel, ve a **Project Settings → Database**.
2. En **Connection string → URI** verás algo como:

```
postgresql://postgres.abc123xyz:[TU-PASSWORD]@aws-0-sa-east-1.pooler.supabase.com:6543/postgres
```

3. Copia esa URI y reemplaza `[TU-PASSWORD]` por la contraseña que pusiste al crear.

> También puedes usar **Connect → ORMs → JDBC** para obtener la URL lista para Spring Boot.

## 3. Crear las tablas

Ve a **SQL Editor → New query** y ejecuta este script:

```sql
create table if not exists habitaciones (
  id           text primary key,          -- S1, D2, M1...
  tipo         text not null,             -- Simple, Doble, Matrimonial
  precio_noche numeric not null,
  estado       text not null default 'Libre'
);

create table if not exists reservas (
  codigo          text primary key,       -- TMP-XXXXXX
  tipo_habitacion text not null,
  nombre          text not null,
  dni             text not null,
  correo          text,
  telefono        text,
  fecha_entrada   date not null,
  fecha_salida    date not null,
  noches          int  not null,
  total           numeric not null,
  estado          text not null default 'Pendiente',
  creado_en       timestamptz default now()
);

create table if not exists pagos (
  id             text primary key,
  codigo_reserva text,
  metodo         text,
  estado         text,
  voucher        text,
  creado_en      timestamptz default now()
);

insert into habitaciones (id, tipo, precio_noche, estado) values
  ('S1','Simple',90,'Libre'),
  ('S2','Simple',90,'Limpieza'),
  ('D1','Doble',140,'Libre'),
  ('D2','Doble',140,'Ocupada'),
  ('M1','Matrimonial',180,'Libre'),
  ('M2','Matrimonial',180,'Limpieza')
on conflict (id) do nothing;
```

## 4. Conectarlo al backend (siguiente paso)

Cuando tengas la URI de conexión, pásamela y yo:

1. Agregaré `spring-boot-starter-data-jpa` + `postgresql` a `reservas-service`.
2. Crearé las entidades y repositorios (reemplazando el almacenamiento en memoria).
3. Configuraré el datasource leyendo `SUPABASE_DB_URL` / `SUPABASE_DB_PASSWORD`
   desde variables de entorno (sin guardar credenciales en el código).

La variable de entorno en Windows se define así:

```powershell
$env:SUPABASE_DB_URL="jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:6543/postgres"
$env:SUPABASE_DB_USER="postgres.abc123xyz"
$env:SUPABASE_DB_PASSWORD="TU-PASSWORD"
```
