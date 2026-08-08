import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CuentaPorCobrarService } from '../../core/services/cuenta-por-cobrar.service';
import { NotificationService } from '../../core/services/notification.service';
import { CuentaPorCobrar } from '../../core/models/cuenta-por-cobrar.model';

@Component({
  selector: 'app-cuenta-list',
  imports: [RouterLink],
  templateUrl: './cuenta-list.html',
  styleUrl: './cuenta-list.css',
})
export class CuentaList {
  private readonly cuentaService = inject(CuentaPorCobrarService);
  private readonly notificationService = inject(NotificationService);

  readonly cuentas = signal<CuentaPorCobrar[]>([]);
  readonly cargando = signal(true);
  readonly filtro = signal('');

  readonly cuentasFiltradas = computed(() => {
    const termino = this.filtro().trim().toLowerCase();
    if (!termino) {
      return this.cuentas();
    }
    return this.cuentas().filter((cuenta) =>
      [
        cuenta.servicio.nombre,
        cuenta.periodo,
        cuenta.estado,
        cuenta.puesto ? `${cuenta.puesto.numero} ${cuenta.puesto.nombreInquilino}` : '',
        cuenta.socio ? `${cuenta.socio.nombres} ${cuenta.socio.apellidos}` : '',
      ]
        .join(' ')
        .toLowerCase()
        .includes(termino),
    );
  });

  constructor() {
    this.cargarCuentas();
  }

  cargarCuentas(): void {
    this.cargando.set(true);
    this.cuentaService.listar().subscribe({
      next: (cuentas) => {
        this.cuentas.set(cuentas);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  eliminar(cuenta: CuentaPorCobrar): void {
    const destinatario = cuenta.puesto
      ? `puesto ${cuenta.puesto.numero}`
      : `socio ${cuenta.socio?.nombres} ${cuenta.socio?.apellidos}`;
    const confirmado = confirm(`¿Eliminar la cuenta por cobrar de ${destinatario} (${cuenta.periodo})?`);
    if (!confirmado) {
      return;
    }

    this.cuentaService.eliminar(cuenta.id).subscribe({
      next: () => {
        this.cuentas.update((lista) => lista.filter((c) => c.id !== cuenta.id));
        this.notificationService.exito('Cuenta por cobrar eliminada correctamente.');
      },
    });
  }
}
