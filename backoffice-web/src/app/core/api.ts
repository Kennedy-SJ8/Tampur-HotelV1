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
}
