import { TestBed } from '@angular/core/testing';
import { DateAdapter } from '@angular/material/core';

import { FechaLocalDateAdapter, provideFechasLocales } from './fecha-adapter';

describe('FechaLocalDateAdapter', () => {
  let adaptador: DateAdapter<Date>;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: provideFechasLocales() });
    adaptador = TestBed.inject(DateAdapter);
  });

  it('se registra como el adaptador de fechas de la aplicacion', () => {
    expect(adaptador).toBeInstanceOf(FechaLocalDateAdapter);
  });

  describe('deserialize', () => {
    it('interpreta "AAAA-MM-DD" en hora local (no se corre un dia)', () => {
      const fecha = adaptador.deserialize('1992-04-10') as Date;

      expect(adaptador.getYear(fecha)).toBe(1992);
      expect(adaptador.getMonth(fecha)).toBe(3);
      expect(adaptador.getDate(fecha)).toBe(10);
    });

    it('delega en el adaptador nativo para valores que no son una fecha ISO simple', () => {
      expect(adaptador.deserialize(null)).toBeNull();
      expect(adaptador.deserialize('')).toBeNull();
    });
  });

  describe('parse', () => {
    it('interpreta dd/MM/aaaa (formato peruano) como dia/mes/anio', () => {
      const fecha = adaptador.parse('10/04/1992', null) as Date;

      expect(adaptador.getDate(fecha)).toBe(10);
      expect(adaptador.getMonth(fecha)).toBe(3);
      expect(adaptador.getYear(fecha)).toBe(1992);
    });

    it('acepta dia y mes de un digito y otros separadores', () => {
      const fecha = adaptador.parse('4-7-2026', null) as Date;

      expect(adaptador.getDate(fecha)).toBe(4);
      expect(adaptador.getMonth(fecha)).toBe(6);
    });

    it('tambien acepta AAAA-MM-DD', () => {
      const fecha = adaptador.parse('2026-08-15', null) as Date;

      expect(adaptador.getDate(fecha)).toBe(15);
      expect(adaptador.getMonth(fecha)).toBe(7);
    });

    it('marca como invalida una fecha que no existe (31/02)', () => {
      const fecha = adaptador.parse('31/02/2026', null) as Date;

      expect(adaptador.isValid(fecha)).toBe(false);
    });

    it('devuelve null para texto vacio', () => {
      expect(adaptador.parse('', null)).toBeNull();
    });
  });

  it('muestra la fecha con formato dd/MM/aaaa', () => {
    const fecha = new Date(1992, 3, 10);
    const formato = { year: 'numeric', month: '2-digit', day: '2-digit' };

    expect(adaptador.format(fecha, formato)).toBe('10/04/1992');
  });
});
