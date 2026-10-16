import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, HabitacionCatalogo } from '../../core/api';

interface TarifaItem {
  tipo: string;
  precioActual: number;
  nuevoPrecio: number;
  cantidad: number;
}

@Component({
  imports: [FormsModule],
  selector: 'app-precios',
  styleUrl: './precios.scss',
  templateUrl: './precios.html',
})
export class Precios implements OnInit {
  private api = inject(ApiService);

  tarifas = signal<TarifaItem[]>([]);
  cargando = signal(true);
  guardando = signal(false);
  mensaje = signal('');
  error = signal('');

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.api.habitacionesCatalogo().subscribe({
      next: habs => {
        const agrupado = new Map<string, { precio: number; cantidad: number }>();
        for (const h of habs) {
          const item = agrupado.get(h.tipo) ?? { precio: h.precioNoche, cantidad: 0 };
          item.cantidad++;
          item.precio = h.precioNoche;
          agrupado.set(h.tipo, item);
        }
        const lista: TarifaItem[] = Array.from(agrupado.entries()).map(([tipo, info]) => ({
          tipo,
          precioActual: info.precio,
          nuevoPrecio: info.precio,
          cantidad: info.cantidad,
        }));
        this.tarifas.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set('No se pudieron cargar las tarifas.');
      }
    });
  }

  guardarTodo() {
    this.guardando.set(true);
    this.mensaje.set('');
    this.error.set('');

    const payload: Record<string, number> = {};
    for (const t of this.tarifas()) {
      payload[t.tipo] = Number(t.nuevoPrecio);
    }

    this.api.actualizarPrecios(payload).subscribe({
      next: () => {
        this.guardando.set(false);
        this.mensaje.set('Precios actualizados correctamente en todas las habitaciones ✅');
        this.cargar();
      },
      error: () => {
        this.guardando.set(false);
        this.error.set('Error al guardar los precios.');
      }
    });
  }
}
