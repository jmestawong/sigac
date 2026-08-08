import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { NotificationService } from '../core/services/notification.service';
import { PagoService } from '../core/services/pago.service';
import { Recibo } from '../core/models/recibo.model';

@Component({
  selector: 'app-ingreso-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './ingreso-form.html',
  styleUrl: './ingreso-form.css',
})
export class IngresoForm {
  private readonly fb = inject(FormBuilder);
  private readonly pagoService = inject(PagoService);
  private readonly notificationService = inject(NotificationService);

  readonly guardando = signal(false);
  readonly ultimoRecibo = signal<Recibo | null>(null);

  readonly form = this.fb.nonNullable.group({
    depositante: ['', Validators.required],
    categoria: ['', Validators.required],
    concepto: ['', Validators.required],
    monto: [null as number | null, [Validators.required, Validators.min(0.01)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    const valores = this.form.getRawValue();

    this.pagoService
      .registrarIngresoExterno({
        depositante: valores.depositante,
        categoria: valores.categoria,
        concepto: valores.concepto,
        monto: valores.monto as number,
      })
      .subscribe({
        next: (recibo) => {
          this.guardando.set(false);
          this.ultimoRecibo.set(recibo);
          this.notificationService.exito(`Ingreso registrado. Recibo ${recibo.correlativo}.`);
          this.form.reset();
        },
        error: () => this.guardando.set(false),
      });
  }
}
