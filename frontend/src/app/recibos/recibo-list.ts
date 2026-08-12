import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PagoService } from '../core/services/pago.service';
import { Recibo, TipoRecibo } from '../core/models/recibo.model';

@Component({
  selector: 'app-recibo-list',
  imports: [RouterLink, DatePipe],
  templateUrl: './recibo-list.html',
  styleUrl: './recibo-list.css',
})
export class ReciboList {
  private readonly pagoService = inject(PagoService);

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
      [recibo.correlativo, recibo.tipo, recibo.depositante, recibo.categoria]
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
