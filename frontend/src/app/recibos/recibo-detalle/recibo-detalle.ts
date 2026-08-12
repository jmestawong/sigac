import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';

import { PagoService } from '../../core/services/pago.service';
import { Recibo } from '../../core/models/recibo.model';

@Component({
  selector: 'app-recibo-detalle',
  imports: [RouterLink, DatePipe],
  templateUrl: './recibo-detalle.html',
  styleUrl: './recibo-detalle.css',
})
export class ReciboDetalle {
  private readonly route = inject(ActivatedRoute);
  private readonly pagoService = inject(PagoService);

  readonly recibo = signal<Recibo | null>(null);
  readonly cargando = signal(true);

  constructor() {
    this.route.paramMap
      .pipe(switchMap((params) => this.pagoService.obtenerRecibo(Number(params.get('id')))))
      .subscribe({
        next: (recibo) => {
          this.recibo.set(recibo);
          this.cargando.set(false);
        },
        error: () => this.cargando.set(false),
      });
  }
}
