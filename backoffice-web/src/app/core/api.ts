import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface Habitacion {
  id: string;
  tipo: string;
  piso: number;
  numero: number;
  estado: string;
  limpieza: string;
}

export interface HabitacionCatalogo {
  id: string;
  tipo: string;
  precioNoche: number;
  estado: string;
}

export interface Reserva {
  codigo: string;
  tipoHabitacion: string;
  nombre: string;
  dni: string;
  correo: string;
  telefono: string;
  fechaEntrada: string;
  fechaSalida: string;
  noches: number;
  total: number;
  estado: string;
  numeroHabitacion: string | null;
  metodoPago: string | null;
  huespedes: number;
  checkinHoy?: boolean;
  checkoutHoy?: boolean;
}

export interface LimpiezaItem {
  id: string;
  tipo: string;
  piso: number;
  numero: number;
  estadoOcupacion: string;
  estadoLimpieza: string;
  prioridad: number;
  horaInicio: string;
  horaFin: string;
  estadoFlujo: string;
}

export interface ReporteIngresos {
  fecha: string;
  ingresosTotales: number;
  reservasConfirmadas: number;
  reservasPendientes: number;
  metodoPrincipal: string;
}

export interface CalendarioDia {
  habitacion: string;
  fecha: string;
  estado: string;
  reserva?: string;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  ocupacion() {
    return this.http.get<Habitacion[]>(`${this.base}/ocupacion`);
  }

  cambiarEstado(id: string, estado: string) {
    return this.http.patch<Habitacion>(`${this.base}/ocupacion/${id}/estado`, { estado });
  }

  habitacionesCatalogo() {
    return this.http.get<HabitacionCatalogo[]>(`${this.base}/habitaciones`);
  }

  actualizarPrecios(precios: Record<string, number>) {
    return this.http.patch<Record<string, number>>(`${this.base}/admin/precios`, precios);
  }

  reservas() {
    return this.http.get<Reserva[]>(`${this.base}/reservas`);
  }

  cambiarEstadoReserva(codigo: string, estado: string) {
    return this.http.patch<{ ok: boolean; reserva: Reserva }>(`${this.base}/reservas/${codigo}/estado`, { estado });
  }

  asignarHabitacionReserva(codigo: string, numeroHabitacion: string) {
    return this.http.patch<Reserva>(`${this.base}/reservas/${codigo}/habitacion`, { numeroHabitacion });
  }

  eliminarReserva(codigo: string) {
    return this.http.delete<void>(`${this.base}/reservas/${codigo}`);
  }

  limpieza() {
    return this.http.get<LimpiezaItem[]>(`${this.base}/limpieza`);
  }

  actualizarLimpieza(numero: number, body: { limpiada?: boolean; estadoFlujo?: string }) {
    return this.http.patch<LimpiezaItem>(`${this.base}/limpieza/${numero}/estado`, body);
  }

  ingresos() {
    return this.http.get<ReporteIngresos>(`${this.base}/reportes/ingresos`);
  }

  calendario(anio: number, mes: number) {
    return this.http.get<CalendarioDia[]>(`${this.base}/calendario?anio=${anio}&mes=${mes}`);
  }

  toggleCalendario(habitacion: string, fecha: string) {
    return this.http.post<{ accion: string; estado: string }>(`${this.base}/calendario/toggle`, { habitacion, fecha });
  }
}
