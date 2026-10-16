import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiService, LimpiezaItem } from '../../core/api';

@Component({
  imports: [],
  selector: 'app-limpieza',
  styleUrl: './limpieza.scss',
  templateUrl: './limpieza.html',
})
export class Limpieza implements OnInit {
  private api = inject(ApiService);

  items = signal<LimpiezaItem[]>([]);
  cargando = signal(true);
  error = signal('');

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.api.limpieza().subscribe({
      next: lista => {
        this.items.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set('No se pudo cargar la lista de limpieza.');
      }
    });
  }

  iniciar(item: LimpiezaItem) {
    this.api.actualizarLimpieza(item.numero, { limpiada: false, estadoFlujo: 'en_curso' }).subscribe({
      next: act => this.items.update(l => l.map(x => (x.numero === act.numero ? act : x))),
      error: () => this.error.set('No se pudo iniciar la limpieza.')
    });
  }

  completar(item: LimpiezaItem) {
    this.api.actualizarLimpieza(item.numero, { limpiada: true, estadoFlujo: 'completada' }).subscribe({
      next: act => this.items.update(l => l.map(x => (x.numero === act.numero ? act : x))),
      error: () => this.error.set('No se pudo marcar como limpiada.')
    });
  }
}
