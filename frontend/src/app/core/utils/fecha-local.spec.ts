import {
  aIsoLocal,
  crearFechaLocal,
  desdeIsoLocal,
  hoyIsoLocal,
  mesActualIsoLocal,
  primerDiaDelMesIsoLocal,
} from './fecha-local';

describe('fecha-local', () => {
  describe('desdeIsoLocal', () => {
    it('interpreta "AAAA-MM-DD" como fecha local, sin correr un dia por UTC', () => {
      const fecha = desdeIsoLocal('1992-04-10');

      expect(fecha).not.toBeNull();
      expect(fecha!.getFullYear()).toBe(1992);
      expect(fecha!.getMonth()).toBe(3);
      expect(fecha!.getDate()).toBe(10);
    });

    it('devuelve null para texto con formato invalido o fechas inexistentes', () => {
      expect(desdeIsoLocal('10/04/1992')).toBeNull();
      expect(desdeIsoLocal('1992-02-31')).toBeNull();
      expect(desdeIsoLocal('')).toBeNull();
    });
  });

  describe('aIsoLocal', () => {
    it('formatea un Date usando sus componentes locales', () => {
      expect(aIsoLocal(new Date(1992, 3, 10))).toBe('1992-04-10');
      expect(aIsoLocal(new Date(2026, 11, 31, 23, 59))).toBe('2026-12-31');
    });

    it('deja pasar un texto ISO y devuelve vacio para null o fechas invalidas', () => {
      expect(aIsoLocal('2026-08-15')).toBe('2026-08-15');
      expect(aIsoLocal(null)).toBe('');
      expect(aIsoLocal(undefined)).toBe('');
      expect(aIsoLocal(new Date('no-es-fecha'))).toBe('');
    });

    it('es la operacion inversa de desdeIsoLocal', () => {
      expect(aIsoLocal(desdeIsoLocal('2026-02-28'))).toBe('2026-02-28');
    });
  });

  describe('crearFechaLocal', () => {
    it('acepta fechas reales y rechaza las que no existen', () => {
      expect(crearFechaLocal(2024, 2, 29)).not.toBeNull();
      expect(crearFechaLocal(2026, 2, 29)).toBeNull();
      expect(crearFechaLocal(2026, 13, 1)).toBeNull();
    });
  });

  describe('fechas relativas a hoy', () => {
    it('hoyIsoLocal, mesActualIsoLocal y primerDiaDelMesIsoLocal son coherentes entre si', () => {
      const hoy = hoyIsoLocal();

      expect(hoy).toMatch(/^\d{4}-\d{2}-\d{2}$/);
      expect(mesActualIsoLocal()).toBe(hoy.slice(0, 7));
      expect(primerDiaDelMesIsoLocal()).toBe(`${hoy.slice(0, 7)}-01`);
    });
  });
});
