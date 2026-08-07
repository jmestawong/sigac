import { Injectable, signal } from '@angular/core';

export interface Notificacion {
  tipo: 'exito' | 'error';
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly notificacionSignal = signal<Notificacion | null>(null);
  readonly notificacion = this.notificacionSignal.asReadonly();
  private timeoutId: ReturnType<typeof setTimeout> | undefined;

  exito(mensaje: string): void {
    this.mostrar({ tipo: 'exito', mensaje });
  }

  error(mensaje: string): void {
    this.mostrar({ tipo: 'error', mensaje });
  }

  limpiar(): void {
    this.notificacionSignal.set(null);
  }

  private mostrar(notificacion: Notificacion): void {
    clearTimeout(this.timeoutId);
    this.notificacionSignal.set(notificacion);
    this.timeoutId = setTimeout(() => this.notificacionSignal.set(null), 4000);
  }
}
