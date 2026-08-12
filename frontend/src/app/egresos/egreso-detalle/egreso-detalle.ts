import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';

import { EgresoService } from '../../core/services/egreso.service';
import { Egreso } from '../../core/models/egreso.model';

@Component({
  selector: 'app-egreso-detalle',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './egreso-detalle.html',
  styleUrl: './egreso-detalle.css',
})
export class EgresoDetalle {
  private readonly route = inject(ActivatedRoute);
  private readonly egresoService = inject(EgresoService);

  readonly egreso = signal<Egreso | null>(null);
  readonly cargando = signal(true);

  constructor() {
    this.route.paramMap
      .pipe(switchMap((params) => this.egresoService.obtener(Number(params.get('id')))))
      .subscribe({
        next: (egreso) => {
          this.egreso.set(egreso);
          this.cargando.set(false);
        },
        error: () => this.cargando.set(false),
      });
  }
}
