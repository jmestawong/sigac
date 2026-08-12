import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { NotificationService } from '../../core/services/notification.service';
import { EgresoService } from '../../core/services/egreso.service';
import { CargaMasivaEgresosResponse } from '../../core/models/egreso.model';

@Component({
  selector: 'app-egreso-carga-masiva',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './egreso-carga-masiva.html',
  styleUrl: './egreso-carga-masiva.css',
})
export class EgresoCargaMasiva {
  private readonly egresoService = inject(EgresoService);
  private readonly notificationService = inject(NotificationService);

  readonly archivo = signal<File | null>(null);
  readonly subiendo = signal(false);
  readonly resultado = signal<CargaMasivaEgresosResponse | null>(null);

  seleccionarArchivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.archivo.set(input.files?.[0] ?? null);
    this.resultado.set(null);
  }

  subir(): void {
    const archivo = this.archivo();
    if (!archivo) {
      return;
    }

    this.subiendo.set(true);
    this.egresoService.registrarMasivo(archivo).subscribe({
      next: (respuesta) => {
        this.subiendo.set(false);
        this.resultado.set(respuesta);
        this.notificationService.exito(
          `Carga procesada: ${respuesta.creados.length} de ${respuesta.totalFilas} filas registradas.`,
        );
      },
      error: () => this.subiendo.set(false),
    });
  }
}
