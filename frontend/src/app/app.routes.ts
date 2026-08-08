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
  {
    path: 'giros',
    canActivate: [authGuard],
    loadComponent: () => import('./giros/giro-list/giro-list').then((m) => m.GiroList),
  },
  {
    path: 'giros/nuevo',
    canActivate: [authGuard],
    loadComponent: () => import('./giros/giro-form/giro-form').then((m) => m.GiroForm),
  },
  {
    path: 'giros/:id/editar',
    canActivate: [authGuard],
    loadComponent: () => import('./giros/giro-form/giro-form').then((m) => m.GiroForm),
  },
  {
    path: 'bancos',
    canActivate: [authGuard],
    loadComponent: () => import('./bancos/banco-list/banco-list').then((m) => m.BancoList),
  },
  {
    path: 'bancos/nuevo',
    canActivate: [authGuard],
    loadComponent: () => import('./bancos/banco-form/banco-form').then((m) => m.BancoForm),
  },
  {
    path: 'bancos/:id/editar',
    canActivate: [authGuard],
    loadComponent: () => import('./bancos/banco-form/banco-form').then((m) => m.BancoForm),
  },
  {
    path: 'puestos',
    canActivate: [authGuard],
    loadComponent: () => import('./puestos/puesto-list/puesto-list').then((m) => m.PuestoList),
  },
  {
    path: 'puestos/nuevo',
    canActivate: [authGuard],
    loadComponent: () => import('./puestos/puesto-form/puesto-form').then((m) => m.PuestoForm),
  },
  {
    path: 'puestos/:id/editar',
    canActivate: [authGuard],
    loadComponent: () => import('./puestos/puesto-form/puesto-form').then((m) => m.PuestoForm),
  },
  {
    path: 'servicios',
    canActivate: [authGuard],
    loadComponent: () => import('./servicios-cobrables/servicio-list/servicio-list').then((m) => m.ServicioList),
  },
  {
    path: 'servicios/nuevo',
    canActivate: [authGuard],
    loadComponent: () => import('./servicios-cobrables/servicio-form/servicio-form').then((m) => m.ServicioForm),
  },
  {
    path: 'servicios/:id/editar',
    canActivate: [authGuard],
    loadComponent: () => import('./servicios-cobrables/servicio-form/servicio-form').then((m) => m.ServicioForm),
  },
  {
    path: 'cuentas-por-cobrar',
    canActivate: [authGuard],
    loadComponent: () => import('./cuentas-por-cobrar/cuenta-list/cuenta-list').then((m) => m.CuentaList),
  },
  {
    path: 'cuentas-por-cobrar/generar',
    canActivate: [authGuard],
    loadComponent: () => import('./cuentas-por-cobrar/generar/generar').then((m) => m.Generar),
  },
  {
    path: 'cobranza',
    canActivate: [authGuard],
    loadComponent: () => import('./cobranza/cobranza').then((m) => m.Cobranza),
  },
  {
    path: 'recibos',
    canActivate: [authGuard],
    loadComponent: () => import('./recibos/recibo-list').then((m) => m.ReciboList),
  },
  {
    path: 'ingresos-externos',
    canActivate: [authGuard],
    loadComponent: () => import('./ingresos-externos/ingreso-form').then((m) => m.IngresoForm),
  },
  { path: '**', redirectTo: 'socios' },
];
