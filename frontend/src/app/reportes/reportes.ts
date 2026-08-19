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
import { MatNativeDateModule } from '@angular/material/core';

interface OpcionReporte {
  tipo: TipoReporte;
  titulo: string;
  descripcion: string;
}

function primerDiaDelMes(): string {
  const hoy = new Date();
  return new Date(hoy.getFullYear(), hoy.getMonth(), 1).toISOString().slice(0, 10);
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
    MatNativeDateModule,
  ],
  templateUrl: './reportes.html',
  styleUrl: './reportes.css',
})
export class Reportes {
  private readonly reporteService = inject(ReporteService);
  private readonly notificationService = inject(NotificationService);

  readonly desde = signal(primerDiaDelMes());
  readonly hasta = signal(new Date().toISOString().slice(0, 10));
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
  // Agrega esto dentro de tu clase Reportes
  formatearFecha(fecha: Date | null | any): string {
    if (!fecha) return '';
    const d = new Date(fecha);
    const mes = ('0' + (d.getMonth() + 1)).slice(-2);
    const dia = ('0' + d.getDate()).slice(-2);
    return `${d.getFullYear()}-${mes}-${dia}`;
  }
}
