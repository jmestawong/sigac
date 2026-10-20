import { Injectable, Provider } from '@angular/core';
import {
  DateAdapter,
  MAT_DATE_FORMATS,
  MAT_DATE_LOCALE,
  MatDateFormats,
  NativeDateAdapter,
} from '@angular/material/core';

import { crearFechaLocal, desdeIsoLocal } from './fecha-local';

/**
 * Adaptador de fechas de Angular Material para el sistema:
 *  - Los valores "AAAA-MM-DD" que llegan del backend se interpretan en hora
 *    local (el adaptador nativo los toma como UTC y en Lima mostraba un dia menos).
 *  - Lo que el usuario escribe se interpreta con el formato peruano dd/MM/aaaa
 *    (tambien acepta AAAA-MM-DD), en lugar del formato mes/dia de Date.parse.
 */
@Injectable()
export class FechaLocalDateAdapter extends NativeDateAdapter {
  override deserialize(valor: unknown): Date | null {
    if (typeof valor === 'string') {
      const fecha = desdeIsoLocal(valor);
      if (fecha) {
        return fecha;
      }
    }
    return super.deserialize(valor);
  }

  override parse(valor: unknown, formato?: unknown): Date | null {
    if (typeof valor === 'string') {
      const texto = valor.trim();

      const iso = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(texto);
      if (iso) {
        return crearFechaLocal(Number(iso[1]), Number(iso[2]), Number(iso[3])) ?? this.invalid();
      }

      const peruano = /^(\d{1,2})[/.-](\d{1,2})[/.-](\d{4})$/.exec(texto);
      if (peruano) {
        return (
          crearFechaLocal(Number(peruano[3]), Number(peruano[2]), Number(peruano[1])) ??
          this.invalid()
        );
      }
    }
    return super.parse(valor, formato);
  }
}

/** Formato de visualizacion dd/MM/aaaa en el campo de entrada del selector de fechas. */
export const FORMATOS_FECHA_PE: MatDateFormats = {
  parse: { dateInput: null },
  display: {
    dateInput: { year: 'numeric', month: '2-digit', day: '2-digit' },
    monthYearLabel: { year: 'numeric', month: 'short' },
    dateA11yLabel: { year: 'numeric', month: 'long', day: 'numeric' },
    monthYearA11yLabel: { year: 'numeric', month: 'long' },
  },
};

/** Proveedores globales del selector de fechas (aplicacion y pruebas). */
export function provideFechasLocales(): Provider[] {
  return [
    { provide: DateAdapter, useClass: FechaLocalDateAdapter },
    { provide: MAT_DATE_LOCALE, useValue: 'es-PE' },
    { provide: MAT_DATE_FORMATS, useValue: FORMATOS_FECHA_PE },
  ];
}
