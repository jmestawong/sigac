import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { NotificationService } from '../../core/services/notification.service';
import { SocioService } from '../../core/services/socio.service';
import { Socio } from '../../core/models/socio.model';

@Component({
  selector: 'app-socio-list',
  imports: [RouterLink],
  templateUrl: './socio-list.html',
  styleUrl: './socio-list.css',
})
export class SocioList {
  private readonly socioService = inject(SocioService);
  private readonly notificationService = inject(NotificationService);

  readonly socios = signal<Socio[]>([]);
  readonly cargando = signal(true);

  constructor() {
    this.cargarSocios();
  }

  cargarSocios(): void {
    this.cargando.set(true);
    this.socioService.listar().subscribe({
      next: (socios) => {
        this.socios.set(socios);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  eliminar(socio: Socio): void {
    const confirmado = confirm(`¿Eliminar al socio "${socio.nombres} ${socio.apellidos}" (${socio.codigo})?`);
    if (!confirmado) {
      return;
    }

    this.socioService.eliminar(socio.id).subscribe({
      next: () => {
        this.socios.update((lista) => lista.filter((s) => s.id !== socio.id));
        this.notificationService.exito('Socio eliminado correctamente.');
      },
    });
  }
}
