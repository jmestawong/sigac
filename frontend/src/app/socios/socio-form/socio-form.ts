import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { NotificationService } from '../../core/services/notification.service';
import { SocioService } from '../../core/services/socio.service';

@Component({
  selector: 'app-socio-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './socio-form.html',
  styleUrl: './socio-form.css',
})
export class SocioForm {
  private readonly fb = inject(FormBuilder);
  private readonly socioService = inject(SocioService);
  private readonly notificationService = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly guardando = signal(false);
  readonly socioId = signal<number | null>(null);
  readonly esEdicion = signal(false);

  readonly form = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    nombres: ['', Validators.required],
    apellidos: ['', Validators.required],
    accion: ['', Validators.required],
    etapa: ['', Validators.required],
    fechaNacimiento: ['', Validators.required],
  });

  private readonly valoresIniciales = this.form.getRawValue();

  constructor() {
    this.route.paramMap.subscribe((params) => {
      const idParam = params.get('id');

      if (!idParam) {
        this.socioId.set(null);
        this.esEdicion.set(false);
        this.form.reset(this.valoresIniciales);
        return;
      }

      const id = Number(idParam);
      this.socioId.set(id);
      this.esEdicion.set(true);

      this.socioService.obtener(id).subscribe((socio) => {
        this.form.patchValue({
          codigo: socio.codigo,
          nombres: socio.nombres,
          apellidos: socio.apellidos,
          accion: socio.accion,
          etapa: socio.etapa,
          fechaNacimiento: socio.fechaNacimiento,
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
    const id = this.socioId();

    const operacion =
      this.esEdicion() && id !== null
        ? this.socioService.actualizar(id, valores)
        : this.socioService.crear(valores);

    operacion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.notificationService.exito(this.esEdicion() ? 'Socio actualizado correctamente.' : 'Socio creado correctamente.');
        this.router.navigate(['/socios']);
      },
      error: () => this.guardando.set(false),
    });
  }
}
