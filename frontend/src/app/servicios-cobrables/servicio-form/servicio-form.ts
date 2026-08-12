import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { NotificationService } from '../../core/services/notification.service';
import { ServicioCobrableService } from '../../core/services/servicio-cobrable.service';
import { Moneda } from '../../core/models/banco.model';
import { Recurrencia, TipoDestinatario } from '../../core/models/servicio-cobrable.model';

@Component({
  selector: 'app-servicio-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './servicio-form.html',
  styleUrl: './servicio-form.css',
})
export class ServicioForm {
  private readonly fb = inject(FormBuilder);
  private readonly servicioService = inject(ServicioCobrableService);
  private readonly notificationService = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly guardando = signal(false);
  readonly servicioId = signal<number | null>(null);
  readonly esEdicion = signal(false);

  readonly form = this.fb.nonNullable.group({
    nombre: ['', Validators.required],
    recurrencia: ['' as Recurrencia | '', Validators.required],
    costo: [null as number | null, [Validators.required, Validators.min(0.01)]],
    moneda: ['' as Moneda | '', Validators.required],
    destinatario: ['' as TipoDestinatario | '', Validators.required],
    esPorConsumo: [false],
  });

  private readonly valoresIniciales = this.form.getRawValue();

  constructor() {
    this.route.paramMap.subscribe((params) => {
      const idParam = params.get('id');

      if (!idParam) {
        this.servicioId.set(null);
        this.esEdicion.set(false);
        this.form.reset(this.valoresIniciales);
        return;
      }

      const id = Number(idParam);
      this.servicioId.set(id);
      this.esEdicion.set(true);

      this.servicioService.obtener(id).subscribe((servicio) => {
        this.form.patchValue({
          nombre: servicio.nombre,
          recurrencia: servicio.recurrencia,
          costo: servicio.costo,
          moneda: servicio.moneda,
          destinatario: servicio.destinatario,
          esPorConsumo: servicio.esPorConsumo,
        });
      });
    });

    this.form.controls.destinatario.valueChanges.subscribe((destinatario) => {
      if (destinatario === 'SOCIO') {
        this.form.controls.esPorConsumo.setValue(false);
        this.form.controls.esPorConsumo.disable();
      } else {
        this.form.controls.esPorConsumo.enable();
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    const valores = this.form.getRawValue();
    const request = {
      nombre: valores.nombre,
      recurrencia: valores.recurrencia as Recurrencia,
      costo: valores.costo as number,
      moneda: valores.moneda as Moneda,
      destinatario: valores.destinatario as TipoDestinatario,
      esPorConsumo: valores.esPorConsumo,
    };
    const id = this.servicioId();

    const operacion =
      this.esEdicion() && id !== null
        ? this.servicioService.actualizar(id, request)
        : this.servicioService.crear(request);

    operacion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.notificationService.exito(
          this.esEdicion() ? 'Servicio actualizado correctamente.' : 'Servicio creado correctamente.',
        );
        this.router.navigate(['/servicios']);
      },
      error: () => this.guardando.set(false),
    });
  }
}
