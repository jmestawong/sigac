import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';

import { RegistroAuditoria } from '../core/models/auditoria.model';
import { AuditoriaService } from '../core/services/auditoria.service';

@Component({
  selector: 'app-auditoria-list',
  imports: [
    DatePipe,
    DecimalPipe,
    MatTableModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatIconModule,
  ],
  templateUrl: './auditoria-list.html',
  styleUrl: './auditoria-list.css',
})
export class AuditoriaList {
  private readonly auditoriaService = inject(AuditoriaService);

  readonly registros = signal<RegistroAuditoria[]>([]);
  readonly cargando = signal(true);
  readonly filtro = signal('');
  readonly columnas = ['fecha', 'usuario', 'modulo', 'accion', 'referencia', 'detalle', 'monto'];

  readonly registrosFiltrados = computed(() => {
    const termino = this.filtro().trim().toLowerCase();
    if (!termino) {
      return this.registros();
    }
    return this.registros().filter((registro) =>
      [registro.usuario, registro.modulo, registro.accion, registro.referencia ?? '', registro.detalle ?? '']
        .join(' ')
        .toLowerCase()
        .includes(termino),
    );
  });

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.auditoriaService.listar().subscribe({
      next: (registros) => {
        this.registros.set(registros);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
