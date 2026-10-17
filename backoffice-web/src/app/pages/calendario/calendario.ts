import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ApiService, CalendarioDia } from '../../core/api';

interface HabCal {
  id: string;
  tipo: string;
  piso: number;
}

@Component({
  imports: [],
  selector: 'app-calendario',
  templateUrl: './calendario.html',
})
export class Calendario implements OnInit {
  private api = inject(ApiService);

  readonly habitaciones: HabCal[] = [
    { id: 'S1', tipo: 'Simple', piso: 1 }, { id: 'S2', tipo: 'Simple', piso: 1 },
    { id: 'M1', tipo: 'Matrimonial', piso: 2 }, { id: 'M2', tipo: 'Matrimonial', piso: 2 },
    { id: 'Q1', tipo: 'Queen', piso: 2 }, { id: 'Q2', tipo: 'Queen', piso: 2 },
    { id: 'K1', tipo: 'King', piso: 3 }, { id: 'K2', tipo: 'King', piso: 3 }
  ];
  readonly meses = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
  readonly diasSemana = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];

  anio = signal(new Date().getFullYear());
  mes = signal(new Date().getMonth() + 1);
  mapa = signal<Record<string, CalendarioDia>>({});
  habActiva = signal<HabCal | null>(null);

  numDias = computed(() => new Date(this.anio(), this.mes(), 0).getDate());

  vaciosInicio = computed(() => {
    const dow = new Date(this.anio(), this.mes() - 1, 1).getDay();
    return Array.from({ length: (dow + 6) % 7 }, (_, i) => i);
  });

  diasMes = computed(() => Array.from({ length: this.numDias() }, (_, i) => i + 1));

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.api.calendario(this.anio(), this.mes()).subscribe({
      next: arr => {
        const m: Record<string, CalendarioDia> = {};
        for (const d of arr) m[`${d.habitacion}_${d.fecha}`] = d;
        this.mapa.set(m);
      },
      error: () => this.mapa.set({})
    });
  }

  nav(delta: number) {
    let m = this.mes() + delta;
    let a = this.anio();
    if (m > 12) { m = 1; a++; }
    if (m < 1) { m = 12; a--; }
    this.mes.set(m);
    this.anio.set(a);
    this.cargar();
  }

  irHoy() {
    const h = new Date();
    this.anio.set(h.getFullYear());
    this.mes.set(h.getMonth() + 1);
    this.cargar();
  }

  fechaStr(d: number) {
    return `${this.anio()}-${String(this.mes()).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
  }

  estadoDia(habId: string, d: number) {
    return this.mapa()[`${habId}_${this.fechaStr(d)}`]?.estado ?? '';
  }

  ocupadosHab(habId: string) {
    let c = 0;
    for (let d = 1; d <= this.numDias(); d++) {
      if (this.estadoDia(habId, d) === 'reservada') c++;
    }
    return c;
  }

  seleccionarHab(h: HabCal) {
    this.habActiva.set(this.habActiva()?.id === h.id ? null : h);
  }

  toggleDia(h: HabCal, d: number) {
    const est = this.estadoDia(h.id, d);
    if (est === 'reservada') return;
    const f = this.fechaStr(d);
    this.api.toggleCalendario(h.id, f).subscribe({
      next: () => this.cargar()
    });
  }
}
