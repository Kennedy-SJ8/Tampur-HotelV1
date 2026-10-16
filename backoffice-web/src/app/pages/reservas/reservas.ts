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
  estados = ['Confirmada', 'Pendiente', 'Cancelada'];

  visibles = computed(() => {
    const f = this.filtro().toLowerCase().trim();
    const lista = [...this.reservas()].sort((a, b) => b.fechaEntrada.localeCompare(a.fechaEntrada));
    if (!f) return lista;
    return lista.filter(r =>
      [r.codigo, r.nombre, r.dni, r.estado].some(v => (v ?? '').toLowerCase().includes(f))
    );
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
      next: () => this.reservas.update(l => l.map(x => (x.codigo === r.codigo ? { ...x, estado } : x))),
      error: () => this.error.set('No se pudo cambiar el estado de la reserva.')
    });
  }

  asignarHabitacion(r: Reserva, numeroHabitacion: string) {
    this.api.asignarHabitacionReserva(r.codigo, numeroHabitacion.trim()).subscribe({
      next: () => this.reservas.update(l => l.map(x => (x.codigo === r.codigo ? { ...x, numeroHabitacion } : x))),
      error: () => this.error.set('No se pudo asignar el número de habitación.')
    });
  }

  eliminar(r: Reserva) {
    if (!confirm(`¿Seguro que deseas eliminar la reserva ${r.codigo}?`)) return;
    this.api.eliminarReserva(r.codigo).subscribe({
      next: () => this.reservas.update(l => l.filter(x => x.codigo !== r.codigo)),
      error: () => this.error.set('No se pudo eliminar la reserva.')
    });
  }

  buscar(texto: string) {
    this.filtro.set(texto);
  }
}
