import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  login(usuario: string, clave: string) {
    return this.http
      .post<{ token: string }>(`${environment.apiUrl}/admin/login`, { usuario, clave })
      .pipe(tap(r => sessionStorage.setItem('token', r.token)));
  }

  logout() {
    sessionStorage.removeItem('token');
  }

  get token() {
    return sessionStorage.getItem('token');
  }

  get logueado() {
    return !!this.token;
  }
}