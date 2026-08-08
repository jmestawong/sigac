import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { NotificationService } from '../../core/services/notification.service';
import { GiroService } from '../../core/services/giro.service';
import { Giro } from '../../core/models/giro.model';

@Component({
  selector: 'app-giro-list',
  imports: [RouterLink],
  templateUrl: './giro-list.html',
  styleUrl: './giro-list.css',
})
export class GiroList {
  private readonly giroService = inject(GiroService);
  private readonly notificationService = inject(NotificationService);

  readonly giros = signal<Giro[]>([]);
  readonly cargando = signal(true);
  readonly filtro = signal('');

  readonly girosFiltrados = computed(() => {
    const termino = this.filtro().trim().toLowerCase();
    if (!termino) {
      return this.giros();
    }
    return this.giros().filter((giro) => giro.nombre.toLowerCase().includes(termino));
  });

  constructor() {
    this.cargarGiros();
  }

  cargarGiros(): void {
    this.cargando.set(true);
    this.giroService.listar().subscribe({
      next: (giros) => {
        this.giros.set(giros);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  eliminar(giro: Giro): void {
    const confirmado = confirm(`¿Eliminar el giro "${giro.nombre}"?`);
    if (!confirmado) {
      return;
    }

    this.giroService.eliminar(giro.id).subscribe({
      next: () => {
        this.giros.update((lista) => lista.filter((g) => g.id !== giro.id));
        this.notificationService.exito('Giro eliminado correctamente.');
      },
    });
  }
}
