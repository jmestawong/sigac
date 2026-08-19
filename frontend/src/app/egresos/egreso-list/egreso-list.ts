import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';

import { NotificationService } from '../../core/services/notification.service';
import { EgresoService } from '../../core/services/egreso.service';
import { Egreso } from '../../core/models/egreso.model';
import { MatIconModule } from '@angular/material/icon';

function primerDiaDelMes(mes: string): string {
  return `${mes}-01`;
}

function ultimoDiaDelMes(mes: string): string {
  const [anio, mesNumero] = mes.split('-').map(Number);
  const ultimoDia = new Date(anio, mesNumero, 0).getDate();
  return `${mes}-${String(ultimoDia).padStart(2, '0')}`;
}

@Component({
  selector: 'app-egreso-list',
  imports: [
    RouterLink,
    DecimalPipe,
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatIconModule,
  ],
  templateUrl: './egreso-list.html',
  styleUrl: './egreso-list.css',
})
export class EgresoList {
  private readonly egresoService = inject(EgresoService);
  private readonly notificationService = inject(NotificationService);

  readonly egresos = signal<Egreso[]>([]);
  readonly cargando = signal(true);
  readonly filtro = signal('');
  readonly mes = signal(new Date().toISOString().slice(0, 7));

  readonly egresosFiltrados = computed(() => {
    const termino = this.filtro().trim().toLowerCase();
    if (!termino) {
      return this.egresos();
    }
    return this.egresos().filter((egreso) =>
      [egreso.correlativo, egreso.numeroDocumento, egreso.proveedor, egreso.motivo]
        .join(' ')
        .toLowerCase()
        .includes(termino),
    );
  });

  constructor() {
    this.cargarEgresos();
  }

  cambiarMes(valor: string): void {
    this.mes.set(valor);
    this.cargarEgresos();
  }

  cargarEgresos(): void {
    this.cargando.set(true);
    const desde = primerDiaDelMes(this.mes());
    const hasta = ultimoDiaDelMes(this.mes());

    this.egresoService.listar(desde, hasta).subscribe({
      next: (egresos) => {
        this.egresos.set(egresos);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  procesar(egreso: Egreso): void {
    const confirmado = confirm(
      `¿Procesar el egreso ${egreso.correlativo}? Esta acción no se puede deshacer.`,
    );
    if (!confirmado) {
      return;
    }

    this.egresoService.procesar(egreso.id).subscribe({
      next: (actualizado) => {
        this.reemplazar(actualizado);
        this.notificationService.exito(
          `Egreso ${actualizado.correlativo} procesado correctamente.`,
        );
      },
    });
  }

  anular(egreso: Egreso): void {
    const confirmado = confirm(
      `¿Anular el egreso ${egreso.correlativo}? Esta acción no se puede deshacer.`,
    );
    if (!confirmado) {
      return;
    }

    this.egresoService.anular(egreso.id).subscribe({
      next: (actualizado) => {
        this.reemplazar(actualizado);
        this.notificationService.exito(`Egreso ${actualizado.correlativo} anulado correctamente.`);
      },
    });
  }

  private reemplazar(egreso: Egreso): void {
    this.egresos.update((lista) => lista.map((e) => (e.id === egreso.id ? egreso : e)));
  }
}
