import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ApiService, Reserva } from '../../core/api';

export interface HuespedResumen {
  dni: string;
  nombre: string;
  correo: string;
  telefono: string;
  visitas: number;
  nochesTotales: number;
  totalGastado: number;
  ultimaVisita: string;
}

@Component({
  imports: [],
  selector: 'app-huespedes',
  styleUrl: './huespedes.scss',
  templateUrl: './huespedes.html',
})
export class Huespedes implements OnInit {
  private api = inject(ApiService);

  reservas = signal<Reserva[]>([]);
  cargando = signal(true);
  error = signal('');
  filtro = signal('');

  huespedes = computed<HuespedResumen[]>(() => {
    const mapa = new Map<string, HuespedResumen>();
    for (const r of this.reservas()) {
      const clave = r.dni || r.correo || r.nombre;
      const actual = mapa.get(clave) ?? {
        dni: r.dni,
        nombre: r.nombre,
        correo: r.correo,
        telefono: r.telefono,
        visitas: 0,
        nochesTotales: 0,
        totalGastado: 0,
        ultimaVisita: r.fechaEntrada,
      };
      actual.visitas += 1;
      actual.nochesTotales += r.noches ?? 0;
      if (r.estado !== 'Cancelada') {
        actual.totalGastado += r.total ?? 0;
      }
      if (r.fechaEntrada > actual.ultimaVisita) {
        actual.ultimaVisita = r.fechaEntrada;
      }
      mapa.set(clave, actual);
    }
    const q = this.filtro().toLowerCase().trim();
    const lista = Array.from(mapa.values()).sort((a, b) => b.totalGastado - a.totalGastado);
    if (!q) return lista;
    return lista.filter(h =>
      [h.nombre, h.dni, h.correo, h.telefono].some(v => (v ?? '').toLowerCase().includes(q))
    );
  });

  ngOnInit() {
    this.api.reservas().subscribe({
      next: lista => {
        this.reservas.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set('No se pudo cargar el directorio de huéspedes.');
      }
    });
  }

  buscar(valor: string) {
    this.filtro.set(valor);
  }
}
