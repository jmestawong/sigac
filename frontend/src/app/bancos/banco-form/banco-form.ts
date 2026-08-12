import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { NotificationService } from '../../core/services/notification.service';
import { BancoService } from '../../core/services/banco.service';

@Component({
  selector: 'app-banco-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './banco-form.html',
  styleUrl: './banco-form.css',
})
export class BancoForm {
  private readonly fb = inject(FormBuilder);
  private readonly bancoService = inject(BancoService);
  private readonly notificationService = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly guardando = signal(false);
  readonly bancoId = signal<number | null>(null);
  readonly esEdicion = signal(false);

  readonly form = this.fb.nonNullable.group({
    nombre: ['', Validators.required],
    numeroCuenta: ['', Validators.required],
    cci: ['', Validators.required],
    moneda: ['' as 'PEN' | 'USD' | '', Validators.required],
  });

  private readonly valoresIniciales = this.form.getRawValue();

  constructor() {
    this.route.paramMap.subscribe((params) => {
      const idParam = params.get('id');

      if (!idParam) {
        this.bancoId.set(null);
        this.esEdicion.set(false);
        this.form.reset(this.valoresIniciales);
        return;
      }

      const id = Number(idParam);
      this.bancoId.set(id);
      this.esEdicion.set(true);

      this.bancoService.obtener(id).subscribe((banco) => {
        this.form.patchValue({
          nombre: banco.nombre,
          numeroCuenta: banco.numeroCuenta,
          cci: banco.cci,
          moneda: banco.moneda,
        });
      });
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    const valores = this.form.getRawValue();
    const bancoRequest = { ...valores, moneda: valores.moneda as 'PEN' | 'USD' };
    const id = this.bancoId();

    const operacion =
      this.esEdicion() && id !== null
        ? this.bancoService.actualizar(id, bancoRequest)
        : this.bancoService.crear(bancoRequest);

    operacion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.notificationService.exito(
          this.esEdicion() ? 'Cuenta bancaria actualizada correctamente.' : 'Cuenta bancaria creada correctamente.',
        );
        this.router.navigate(['/bancos']);
      },
      error: () => this.guardando.set(false),
    });
  }
}
