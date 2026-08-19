import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { NotificationService } from '../../core/services/notification.service';
import { EgresoService } from '../../core/services/egreso.service';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-egreso-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
  ],
  templateUrl: './egreso-form.html',
  styleUrl: './egreso-form.css',
})
export class EgresoForm {
  private readonly fb = inject(FormBuilder);
  private readonly egresoService = inject(EgresoService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly guardando = signal(false);

  readonly form = this.fb.nonNullable.group({
    numeroDocumento: ['', Validators.required],
    proveedor: ['', Validators.required],
    fecha: [new Date().toISOString().slice(0, 10), Validators.required],
    importe: [0, [Validators.required, Validators.min(0.01)]],
    documentoAsociado: [''],
    motivo: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    const valores = this.form.getRawValue();

    this.egresoService
      .registrar({
        numeroDocumento: valores.numeroDocumento,
        proveedor: valores.proveedor,
        fecha: valores.fecha,
        importe: valores.importe,
        documentoAsociado: valores.documentoAsociado || null,
        motivo: valores.motivo,
      })
      .subscribe({
        next: (egreso) => {
          this.guardando.set(false);
          this.notificationService.exito(`Egreso ${egreso.correlativo} registrado correctamente.`);
          this.router.navigate(['/egresos']);
        },
        error: () => this.guardando.set(false),
      });
  }
}
