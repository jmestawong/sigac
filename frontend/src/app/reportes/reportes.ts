import { Component, inject, signal } from '@angular/core';

import { NotificationService } from '../core/services/notification.service';
import { ReporteService, TipoReporte } from '../core/services/reporte.service';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { aIsoLocal, hoyIsoLocal, primerDiaDelMesIsoLocal } from '../core/utils/fecha-local';

interface OpcionReporte {
  tipo: TipoReporte;
  titulo: string;
  descripcion: string;
}

@Component({
  selector: 'app-reportes',
  imports: [
    CommonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatDatepickerModule,
  ],
  templateUrl: './reportes.html',
  styleUrl: './reportes.css',
})
export class Reportes {
  private readonly reporteService = inject(ReporteService);
  private readonly notificationService = inject(NotificationService);

  readonly desde = signal(primerDiaDelMesIsoLocal());
  readonly hasta = signal(hoyIsoLocal());
  readonly descargando = signal<TipoReporte | null>(null);

  readonly opciones: OpcionReporte[] = [
    {
      tipo: 'movimientos',
      titulo: 'Movimientos diarios',
      descripcion: 'Ingresos y egresos del periodo en dos hojas.',
    },
    {
      tipo: 'totales',
      titulo: 'Totales del periodo',
      descripcion: 'Resumen de totales de ingresos, egresos y saldo.',
    },
    {
      tipo: 'socios',
      titulo: 'Movimientos de socios',
      descripcion: 'Pagos de cuentas asociadas directamente a un socio.',
    },
    {
      tipo: 'no-socios',
      titulo: 'Movimientos de puestos',
      descripcion: 'Pagos de cuentas asociadas a un puesto (no socios).',
    },
    {
      tipo: 'egresos',
      titulo: 'Egresos',
      descripcion: 'Detalle de egresos registrados en el periodo.',
    },
    {
      tipo: 'bancos',
      titulo: 'Recibos bancarios',
      descripcion: 'Canjes bancarios registrados en el periodo.',
    },
  ];

  descargar(opcion: OpcionReporte): void {
    const desde = this.desde();
    const hasta = this.hasta();

    if (!desde || !hasta || desde > hasta) {
      this.notificationService.error('El rango de fechas no es válido.');
      return;
    }

    this.descargando.set(opcion.tipo);
    this.reporteService.descargar(opcion.tipo, desde, hasta).subscribe({
      next: (blob) => {
        this.reporteService.guardarArchivo(blob, opcion.tipo, desde, hasta);
        this.descargando.set(null);
      },
      error: () => {
        this.notificationService.error(`No se pudo generar el reporte "${opcion.titulo}".`);
        this.descargando.set(null);
      },
    });
  }

  formatearFecha(fecha: Date | string | null): string {
    return aIsoLocal(fecha);
  }
}
