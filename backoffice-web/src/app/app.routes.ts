import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Dashboard } from './pages/dashboard/dashboard';
import { Reservas } from './pages/reservas/reservas';
import { Precios } from './pages/precios/precios';
import { Huespedes } from './pages/huespedes/huespedes';
import { Limpieza } from './pages/limpieza/limpieza';
import { Calendario } from './pages/calendario/calendario';
import { authGuard } from './core/auth-guard';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'reservas', component: Reservas, canActivate: [authGuard] },
  { path: 'calendario', component: Calendario, canActivate: [authGuard] },
  { path: 'dashboard', component: Dashboard, canActivate: [authGuard] },
  { path: 'limpieza', component: Limpieza, canActivate: [authGuard] },
  { path: 'precios', component: Precios, canActivate: [authGuard] },
  { path: 'huespedes', component: Huespedes, canActivate: [authGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'reservas' },
  { path: '**', redirectTo: 'reservas' }
];
