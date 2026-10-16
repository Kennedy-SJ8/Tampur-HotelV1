import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth';
import { ApiService, Habitacion, ReporteIngresos, Reserva } from '../../core/api';

@Component({
  imports: [],
  selector: 'app-dashboard',
  styleUrl: './dashboard.scss',
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  private auth = inject(AuthService);
  private api = inject(ApiService);
  private router = inject(Router);

  habitaciones = signal<Habitacion[]>([]);
  reporte = signal<ReporteIngresos | null>(null);
  reservas = signal<Reserva[]>([]);
  cargando = signal(true);
  error = signal('');
  estados = ['Libre', 'Ocupada', 'Limpieza', 'Mantenimiento'];

  resumen = computed(() => {
    const lista = this.habitaciones();
    const cuenta = (e: string) => lista.filter(h => h.estado === e).length;
    const total = lista.length || 1;
    const libres = cuenta('Libre');
    const ocupadas = cuenta('Ocupada');
    const limpieza = cuenta('Limpieza');
    const mantenimiento = cuenta('Mantenimiento');
    return {
      total: lista.length,
      libres,
      ocupadas,
      limpieza,
      mantenimiento,
      pctOcupacion: Math.round((ocupadas / total) * 100),
      pctLibres: Math.round((libres / total) * 100),
      pctLimpieza: Math.round((limpieza / total) * 100),
      pctMantenimiento: Math.round((mantenimiento / total) * 100),
    };
  });

  ingresosPorTipo = computed(() => {
    const tipos = ['Simple', 'Matrimonial', 'Doble', 'Queen', 'King'];
    const mapa = new Map<string, number>();
    for (const r of this.reservas()) {
      if (r.estado === 'Confirmada' || r.estado === 'Pagada') {
        const t = r.tipoHabitacion || 'Otro';
        mapa.set(t, (mapa.get(t) ?? 0) + (r.total ?? 0));
      }
    }
    for (const t of tipos) {
      if (!mapa.has(t)) mapa.set(t, 0);
    }
    const entradas = Array.from(mapa.entries()).filter(([t, v]) => v > 0 || ['Simple', 'Matrimonial', 'Doble'].includes(t));
    const max = Math.max(...entradas.map(([, v]) => v), 1);
    return entradas.map(([tipo, monto]) => ({
      tipo,
      monto,
      pct: Math.round((monto / max) * 100)
    }));
  });

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.api.ocupacion().subscribe({
      next: lista => {
        this.habitaciones.set(lista);
        this.cargando.set(false);
      },
      error: e => {
        this.cargando.set(false);
        if (e.status === 401) {
          this.salir();
        } else {
          this.error.set('No se pudo cargar la ocupación.');
        }
      }
    });

    this.api.ingresos().subscribe({
      next: rep => this.reporte.set(rep),
      error: () => {}
    });

    this.api.reservas().subscribe({
      next: res => this.reservas.set(res),
      error: () => {}
    });
  }

  cambiarEstado(h: Habitacion, estado: string) {
    this.api.cambiarEstado(h.id, estado).subscribe({
      next: act => this.habitaciones.update(l => l.map(x => (x.id === act.id ? act : x))),
      error: () => this.error.set('No se pudo cambiar el estado.')
    });
  }

  salir() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
