import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'socios' },
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login').then((m) => m.Login),
  },
  {
    path: 'socios',
    canActivate: [authGuard],
    loadComponent: () => import('./socios/socio-list/socio-list').then((m) => m.SocioList),
  },
  {
    path: 'socios/nuevo',
    canActivate: [authGuard],
    loadComponent: () => import('./socios/socio-form/socio-form').then((m) => m.SocioForm),
  },
  {
    path: 'socios/:id/editar',
    canActivate: [authGuard],
    loadComponent: () => import('./socios/socio-form/socio-form').then((m) => m.SocioForm),
  },
  { path: '**', redirectTo: 'socios' },
];
