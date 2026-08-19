import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink, RouterModule } from '@angular/router';

import { NotificationService } from '../../core/services/notification.service';
import { GiroService } from '../../core/services/giro.service';
import { PuestoService } from '../../core/services/puesto.service';
import { SocioService } from '../../core/services/socio.service';
import { Giro } from '../../core/models/giro.model';
import { Socio } from '../../core/models/socio.model';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

function vigenciaValida(control: AbstractControl): ValidationErrors | null {
  const inicio = control.get('fechaInicioVigencia')?.value;
  const fin = control.get('fechaFinVigencia')?.value;

  if (!inicio || !fin) {
    return null;
  }

  return fin < inicio ? { vigenciaInvalida: true } : null;
}

@Component({
  selector: 'app-puesto-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
  templateUrl: './puesto-form.html',
  styleUrl: './puesto-form.css',
})
export class PuestoForm {
  private readonly fb = inject(FormBuilder);
  private readonly puestoService = inject(PuestoService);
  private readonly giroService = inject(GiroService);
  private readonly socioService = inject(SocioService);
  private readonly notificationService = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly guardando = signal(false);
  readonly puestoId = signal<number | null>(null);
  readonly esEdicion = signal(false);
  readonly giros = signal<Giro[]>([]);
  readonly socios = signal<Socio[]>([]);

  readonly form = this.fb.nonNullable.group(
    {
      numero: ['', Validators.required],
      nombreInquilino: ['', Validators.required],
      fechaInicioVigencia: ['', Validators.required],
      fechaFinVigencia: ['', Validators.required],
      giroId: ['' as number | '', Validators.required],
      socioId: [null as number | null],
    },
    { validators: vigenciaValida },
  );

  private readonly valoresIniciales = this.form.getRawValue();

  constructor() {
    this.giroService.listar().subscribe((giros) => this.giros.set(giros));
    this.socioService.listar().subscribe((socios) => this.socios.set(socios));

    this.route.paramMap.subscribe((params) => {
      const idParam = params.get('id');

      if (!idParam) {
        this.puestoId.set(null);
        this.esEdicion.set(false);
        this.form.reset(this.valoresIniciales);
        return;
      }

      const id = Number(idParam);
      this.puestoId.set(id);
      this.esEdicion.set(true);

      this.puestoService.obtener(id).subscribe((puesto) => {
        this.form.patchValue({
          numero: puesto.numero,
          nombreInquilino: puesto.nombreInquilino,
          fechaInicioVigencia: puesto.fechaInicioVigencia,
          fechaFinVigencia: puesto.fechaFinVigencia,
          giroId: puesto.giro.id,
          socioId: puesto.socio ? puesto.socio.id : null,
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
    const puestoRequest = {
      numero: valores.numero,
      nombreInquilino: valores.nombreInquilino,
      fechaInicioVigencia: valores.fechaInicioVigencia,
      fechaFinVigencia: valores.fechaFinVigencia,
      giroId: Number(valores.giroId),
      socioId: valores.socioId ? Number(valores.socioId) : null,
    };
    const id = this.puestoId();

    const operacion =
      this.esEdicion() && id !== null
        ? this.puestoService.actualizar(id, puestoRequest)
        : this.puestoService.crear(puestoRequest);

    operacion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.notificationService.exito(
          this.esEdicion() ? 'Puesto actualizado correctamente.' : 'Puesto creado correctamente.',
        );
        this.router.navigate(['/puestos']);
      },
      error: () => this.guardando.set(false),
    });
  }
}
