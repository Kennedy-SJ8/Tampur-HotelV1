-- ============================================
-- Hotel Támpur - Schema para Supabase (PostgreSQL)
-- Ejecuta este script en el SQL Editor de Supabase
-- ============================================

-- Tabla de habitaciones
create table if not exists habitaciones (
  id           text primary key,          -- S1, S2, D1, D2, M1, M2
  tipo         text not null,             -- Simple, Doble, Matrimonial
  precio_noche numeric not null,
  estado       text not null default 'Libre'
);

-- Tabla de reservas
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

-- Tabla de pagos
create table if not exists pagos (
  id             text primary key,
  codigo_reserva text,
  metodo         text,
  estado         text,
  voucher        text,
  creado_en      timestamptz default now()
);

-- Insertar habitaciones iniciales
insert into habitaciones (id, tipo, precio_noche, estado) values
  ('S1','Simple',90,'Libre'),
  ('S2','Simple',90,'Libre'),
  ('D1','Doble',140,'Libre'),
  ('D2','Doble',140,'Libre'),
  ('M1','Matrimonial',180,'Libre'),
  ('M2','Matrimonial',180,'Libre')
on conflict (id) do nothing;
