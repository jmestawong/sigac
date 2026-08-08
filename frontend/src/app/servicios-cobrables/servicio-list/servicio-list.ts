import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { NotificationService } from '../../core/services/notification.service';
import { ServicioCobrableService } from '../../core/services/servicio-cobrable.service';
import { ServicioCobrable } from '../../core/models/servicio-cobrable.model';

@Component({
  selector: 'app-servicio-list',
  imports: [RouterLink],
  templateUrl: './servicio-list.html',
  styleUrl: './servicio-list.css',
})
export class ServicioList {
  private readonly servicioService = inject(ServicioCobrableService);
  private readonly notificationService = inject(NotificationService);

  readonly servicios = signal<ServicioCobrable[]>([]);
  readonly cargando = signal(true);
  readonly filtro = signal('');

  readonly serviciosFiltrados = computed(() => {
    const termino = this.filtro().trim().toLowerCase();
    if (!termino) {
      return this.servicios();
    }
    return this.servicios().filter((servicio) =>
      [servicio.nombre, servicio.recurrencia, servicio.destinatario, servicio.moneda]
        .join(' ')
        .toLowerCase()
        .includes(termino),
    );
  });

  constructor() {
    this.cargarServicios();
  }

  cargarServicios(): void {
    this.cargando.set(true);
    this.servicioService.listar().subscribe({
      next: (servicios) => {
        this.servicios.set(servicios);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  eliminar(servicio: ServicioCobrable): void {
    const confirmado = confirm(`¿Eliminar el servicio "${servicio.nombre}"?`);
    if (!confirmado) {
      return;
    }

    this.servicioService.eliminar(servicio.id).subscribe({
      next: () => {
        this.servicios.update((lista) => lista.filter((s) => s.id !== servicio.id));
        this.notificationService.exito('Servicio eliminado correctamente.');
      },
    });
  }
}
