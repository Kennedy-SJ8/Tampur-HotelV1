import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ApiService, Reserva } from '../../core/api';

@Component({
  imports: [],
  selector: 'app-reservas',
  styleUrl: './reservas.scss',
  templateUrl: './reservas.html',
})
export class Reservas implements OnInit {
  private api = inject(ApiService);

  reservas = signal<Reserva[]>([]);
  cargando = signal(true);
  error = signal('');
  filtro = signal('');
  estadoFiltro = signal('');

  kpis = computed(() => {
    const rs = this.reservas();
    const conf = rs.filter(r => r.estado === 'Confirmada');
    return {
      confirmadas: conf.length,
      pendientes: rs.filter(r => r.estado === 'Pendiente').length,
      canceladas: rs.filter(r => r.estado === 'Cancelada').length,
      ingresos: conf.reduce((s, r) => s + (r.total ?? 0), 0),
    };
  });

  visibles = computed(() => {
    const f = this.filtro().toLowerCase().trim();
    const ef = this.estadoFiltro();
    const lista = [...this.reservas()].sort((a, b) => b.fechaEntrada.localeCompare(a.fechaEntrada));
    return lista.filter(r => {
      const okEst = !ef || r.estado === ef;
      const okTxt = !f || [r.codigo, r.nombre, r.dni, r.tipoHabitacion].some(v => (v ?? '').toLowerCase().includes(f));
      return okEst && okTxt;
    });
  });

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.api.reservas().subscribe({
      next: lista => {
        this.reservas.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set('No se pudieron cargar las reservas.');
      }
    });
  }

  cambiarEstado(r: Reserva, estado: string) {
    this.api.cambiarEstadoReserva(r.codigo, estado).subscribe({
      next: res => {
        const actualizada = res?.reserva ?? { ...r, estado };
        this.reservas.update(l => l.map(x => (x.codigo === r.codigo ? { ...x, ...actualizada, estado } : x)));
      },
      error: () => this.error.set('No se pudo cambiar el estado de la reserva.')
    });
  }

  asignarHabitacion(r: Reserva, numeroHabitacion: string) {
    const num = numeroHabitacion.trim();
    if (!num) return;
    this.api.asignarHabitacionReserva(r.codigo, num).subscribe({
      next: () => this.reservas.update(l => l.map(x => (x.codigo === r.codigo ? { ...x, numeroHabitacion: num } : x))),
      error: () => this.error.set('No se pudo asignar el número de habitación.')
    });
  }

  eliminar(r: Reserva) {
    if (!confirm(`¿Eliminar permanentemente la reserva ${r.codigo}?`)) return;
    this.api.eliminarReserva(r.codigo).subscribe({
      next: () => this.reservas.update(l => l.filter(x => x.codigo !== r.codigo)),
      error: () => this.error.set('No se pudo eliminar la reserva.')
    });
  }

  buscar(texto: string) {
    this.filtro.set(texto);
  }

  filtrarEstado(est: string) {
    this.estadoFiltro.set(est);
  }
}
