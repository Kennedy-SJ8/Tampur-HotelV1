import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  private auth = inject(AuthService);
  private router = inject(Router);

  get logueado() {
    return this.auth.logueado;
  }

  salir() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
