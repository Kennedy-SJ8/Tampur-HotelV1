import { Component, OnInit, inject, signal, computed } from '@angular/core';
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

  kpis = computed(() => {
    const arr = this.items();
    const total = arr.length;
    const limpiadas = arr.filter(x => x.estadoLimpieza === 'limpiada' || x.estadoFlujo === 'completada').length;
    return {
      total,
      limpiadas,
      pendientes: total - limpiadas,
      pct: total ? Math.round((limpiadas / total) * 100) : 0
    };
  });

  porPiso = computed(() => {
    const mapa = new Map<number, LimpiezaItem[]>();
    for (const it of this.items()) {
      const l = mapa.get(it.piso) ?? [];
      l.push(it);
      mapa.set(it.piso, l);
    }
    return Array.from(mapa.entries())
      .sort(([a], [b]) => a - b)
      .map(([piso, habs]) => {
        const limp = habs.filter(x => x.estadoLimpieza === 'limpiada' || x.estadoFlujo === 'completada').length;
        return {
          piso,
          habs: [...habs].sort((a, b) => a.numero - b.numero),
          limpiadas: limp,
          total: habs.length,
          pct: habs.length ? Math.round((limp / habs.length) * 100) : 0
        };
      });
  });

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

  imprimir() {
    window.print();
  }
}
