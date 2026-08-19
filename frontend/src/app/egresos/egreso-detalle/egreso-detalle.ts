import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';

import { EgresoService } from '../../core/services/egreso.service';
import { Egreso } from '../../core/models/egreso.model';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-egreso-detalle',
  imports: [
    RouterLink,
    DecimalPipe,
    CommonModule,
    MatDividerModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
  ],
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
