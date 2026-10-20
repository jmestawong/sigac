import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

import { PagoService } from '../core/services/pago.service';
import { Recibo, TipoRecibo } from '../core/models/recibo.model';

/** Texto del destinatario de un recibo segun su tipo (socio, puesto o depositante). */
export function destinatarioDeRecibo(recibo: Recibo): string {
  if (recibo.tipo === 'INGRESO_EXTERNO') {
    return recibo.depositante ?? '';
  }

  const nombres = (recibo.cuentas ?? []).map((cuenta) =>
    cuenta.puesto
      ? `${cuenta.puesto.numero} — ${cuenta.puesto.nombreInquilino}`
      : cuenta.socio
        ? `${cuenta.socio.nombres} ${cuenta.socio.apellidos}`
        : '',
  );

  return [...new Set(nombres.filter(Boolean))].join(', ');
}

@Component({
  selector: 'app-recibo-list',
  imports: [
    RouterLink,
    DatePipe,
    DecimalPipe,
    MatTableModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
  ],
  templateUrl: './recibo-list.html',
  styleUrl: './recibo-list.css',
})
export class ReciboList {
  private readonly pagoService = inject(PagoService);

  protected readonly destinatarioDe = destinatarioDeRecibo;

  readonly recibos = signal<Recibo[]>([]);
  readonly cargando = signal(true);
  readonly filtro = signal('');
  readonly fecha = signal('');
  readonly tipo = signal<TipoRecibo | ''>('');

  readonly recibosFiltrados = computed(() => {
    const termino = this.filtro().trim().toLowerCase();
    if (!termino) {
      return this.recibos();
    }
    return this.recibos().filter((recibo) =>
      [recibo.correlativo, recibo.tipo, recibo.depositante, recibo.categoria, destinatarioDeRecibo(recibo)]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(termino),
    );
  });

  constructor() {
    this.cargarRecibos();
  }

  cambiarFecha(valor: string): void {
    this.fecha.set(valor);
    this.cargarRecibos();
  }

  cambiarTipo(valor: string): void {
    this.tipo.set(valor as TipoRecibo | '');
    this.cargarRecibos();
  }

  limpiarFiltros(): void {
    this.fecha.set('');
    this.tipo.set('');
    this.filtro.set('');
    this.cargarRecibos();
  }

  cargarRecibos(): void {
    this.cargando.set(true);
    const tipo = this.tipo();

    this.pagoService.listarRecibos(this.fecha() || undefined, tipo || undefined).subscribe({
      next: (recibos) => {
        this.recibos.set(recibos);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
