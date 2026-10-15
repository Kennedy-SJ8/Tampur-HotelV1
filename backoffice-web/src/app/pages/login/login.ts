import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth';

@Component({
  imports: [FormsModule],
  selector: 'app-login',
  styleUrl: './login.scss',
  templateUrl: './login.html',
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);

  usuario = '';
  clave = '';
  error = '';
  cargando = false;

  entrar() {
    this.error = '';
    this.cargando = true;
    this.auth.login(this.usuario, this.clave).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: () => {
        this.cargando = false;
        this.error = 'Usuario o contraseña incorrectos (o el servidor está despertando, intenta de nuevo).';
      }
    });
  }
}
