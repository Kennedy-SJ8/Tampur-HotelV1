import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth';
import { ApiService, Habitacion } from '../../core/api';

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
  cargando = signal(true);
  error = signal('');
  estados = ['Libre', 'Ocupada', 'Limpieza', 'Mantenimiento'];

  resumen = computed(() => {
    const lista = this.habitaciones();
    const cuenta = (e: string) => lista.filter(h => h.estado === e).length;
    return {
      total: lista.length,
      libres: cuenta('Libre'),
      ocupadas: cuenta('Ocupada'),
      limpieza: cuenta('Limpieza'),
      mantenimiento: cuenta('Mantenimiento'),
    };
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
