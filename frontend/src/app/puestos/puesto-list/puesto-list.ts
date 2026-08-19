import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterModule } from '@angular/router';

import { NotificationService } from '../../core/services/notification.service';
import { PuestoService } from '../../core/services/puesto.service';
import { Puesto } from '../../core/models/puesto.model';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';

@Component({
  selector: 'app-puesto-list',
  imports: [
    RouterLink,
    CommonModule,
    RouterModule,
    MatTableModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
  ],
  templateUrl: './puesto-list.html',
  styleUrl: './puesto-list.css',
})
export class PuestoList {
  private readonly puestoService = inject(PuestoService);
  private readonly notificationService = inject(NotificationService);

  readonly puestos = signal<Puesto[]>([]);
  readonly cargando = signal(true);
  readonly filtro = signal('');

  readonly puestosFiltrados = computed(() => {
    const termino = this.filtro().trim().toLowerCase();
    if (!termino) {
      return this.puestos();
    }
    return this.puestos().filter((puesto) =>
      [
        puesto.numero,
        puesto.nombreInquilino,
        puesto.giro.nombre,
        puesto.socio ? `${puesto.socio.nombres} ${puesto.socio.apellidos}` : '',
      ]
        .join(' ')
        .toLowerCase()
        .includes(termino),
    );
  });

  constructor() {
    this.cargarPuestos();
  }

  cargarPuestos(): void {
    this.cargando.set(true);
    this.puestoService.listar().subscribe({
      next: (puestos) => {
        this.puestos.set(puestos);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  eliminar(puesto: Puesto): void {
    const confirmado = confirm(`¿Eliminar el puesto "${puesto.numero}"?`);
    if (!confirmado) {
      return;
    }

    this.puestoService.eliminar(puesto.id).subscribe({
      next: () => {
        this.puestos.update((lista) => lista.filter((p) => p.id !== puesto.id));
        this.notificationService.exito('Puesto eliminado correctamente.');
      },
    });
  }
}
