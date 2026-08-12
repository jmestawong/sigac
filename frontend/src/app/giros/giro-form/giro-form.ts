import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { NotificationService } from '../../core/services/notification.service';
import { GiroService } from '../../core/services/giro.service';

@Component({
  selector: 'app-giro-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './giro-form.html',
  styleUrl: './giro-form.css',
})
export class GiroForm {
  private readonly fb = inject(FormBuilder);
  private readonly giroService = inject(GiroService);
  private readonly notificationService = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly guardando = signal(false);
  readonly giroId = signal<number | null>(null);
  readonly esEdicion = signal(false);

  readonly form = this.fb.nonNullable.group({
    nombre: ['', Validators.required],
  });

  private readonly valoresIniciales = this.form.getRawValue();

  constructor() {
    this.route.paramMap.subscribe((params) => {
      const idParam = params.get('id');

      if (!idParam) {
        this.giroId.set(null);
        this.esEdicion.set(false);
        this.form.reset(this.valoresIniciales);
        return;
      }

      const id = Number(idParam);
      this.giroId.set(id);
      this.esEdicion.set(true);

      this.giroService.obtener(id).subscribe((giro) => {
        this.form.patchValue({ nombre: giro.nombre });
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
    const id = this.giroId();

    const operacion =
      this.esEdicion() && id !== null ? this.giroService.actualizar(id, valores) : this.giroService.crear(valores);

    operacion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.notificationService.exito(this.esEdicion() ? 'Giro actualizado correctamente.' : 'Giro creado correctamente.');
        this.router.navigate(['/giros']);
      },
      error: () => this.guardando.set(false),
    });
  }
}
