import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { NotificationService } from '../../core/services/notification.service';
import { BancoService } from '../../core/services/banco.service';
import { Banco } from '../../core/models/banco.model';

@Component({
  selector: 'app-banco-list',
  imports: [RouterLink],
  templateUrl: './banco-list.html',
  styleUrl: './banco-list.css',
})
export class BancoList {
  private readonly bancoService = inject(BancoService);
  private readonly notificationService = inject(NotificationService);

  readonly bancos = signal<Banco[]>([]);
  readonly cargando = signal(true);
  readonly filtro = signal('');

  readonly bancosFiltrados = computed(() => {
    const termino = this.filtro().trim().toLowerCase();
    if (!termino) {
      return this.bancos();
    }
    return this.bancos().filter((banco) =>
      [banco.nombre, banco.numeroCuenta, banco.cci, banco.moneda].join(' ').toLowerCase().includes(termino),
    );
  });

  constructor() {
    this.cargarBancos();
  }

  cargarBancos(): void {
    this.cargando.set(true);
    this.bancoService.listar().subscribe({
      next: (bancos) => {
        this.bancos.set(bancos);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  eliminar(banco: Banco): void {
    const confirmado = confirm(`¿Eliminar la cuenta "${banco.nombre} - ${banco.numeroCuenta}"?`);
    if (!confirmado) {
      return;
    }

    this.bancoService.eliminar(banco.id).subscribe({
      next: () => {
        this.bancos.update((lista) => lista.filter((b) => b.id !== banco.id));
        this.notificationService.exito('Cuenta bancaria eliminada correctamente.');
      },
    });
  }
}
