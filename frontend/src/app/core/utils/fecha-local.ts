/**
 * Utilidades para trabajar con fechas "de calendario" (sin hora) en la zona
 * horaria local del usuario.
 *
 * Por que existen: el backend maneja las fechas como LocalDate ("AAAA-MM-DD").
 * Si en el navegador se usa Date.parse("1992-04-10") o toISOString(), JavaScript
 * interpreta/serializa en UTC, y en Lima (UTC-5) la fecha se corre un dia
 * (se mostraba 09/04/1992 en lugar de 10/04/1992, y despues de las 19:00 el
 * "hoy" resultaba ser el dia siguiente). Estas funciones usan siempre los
 * componentes locales (anio, mes, dia) para evitar ese desfase.
 */

const dosDigitos = (n: number): string => String(n).padStart(2, '0');

/** Crea una fecha local; devuelve null si la combinacion no existe (ej. 31/02). */
export function crearFechaLocal(anio: number, mes: number, dia: number): Date | null {
  const fecha = new Date(anio, mes - 1, dia);
  const valida =
    fecha.getFullYear() === anio && fecha.getMonth() === mes - 1 && fecha.getDate() === dia;
  return valida ? fecha : null;
}

/** "AAAA-MM-DD" -> Date en hora local (a medianoche local, sin corrimiento por UTC). */
export function desdeIsoLocal(valor: string): Date | null {
  const coincidencia = /^(\d{4})-(\d{2})-(\d{2})$/.exec(valor);
  return coincidencia
    ? crearFechaLocal(Number(coincidencia[1]), Number(coincidencia[2]), Number(coincidencia[3]))
    : null;
}

/**
 * Date | "AAAA-MM-DD" -> "AAAA-MM-DD" usando los componentes locales.
 * Un texto ya en formato ISO se devuelve tal cual; null/invalido -> cadena vacia.
 */
export function aIsoLocal(valor: Date | string | null | undefined): string {
  if (!valor) {
    return '';
  }
  if (typeof valor === 'string') {
    return valor.slice(0, 10);
  }
  if (Number.isNaN(valor.getTime())) {
    return '';
  }
  return `${valor.getFullYear()}-${dosDigitos(valor.getMonth() + 1)}-${dosDigitos(valor.getDate())}`;
}

/** Fecha de hoy en hora local, como "AAAA-MM-DD". */
export function hoyIsoLocal(): string {
  return aIsoLocal(new Date());
}

/** Mes actual en hora local, como "AAAA-MM". */
export function mesActualIsoLocal(): string {
  return hoyIsoLocal().slice(0, 7);
}

/** Primer dia del mes actual en hora local, como "AAAA-MM-DD". */
export function primerDiaDelMesIsoLocal(): string {
  const hoy = new Date();
  return aIsoLocal(new Date(hoy.getFullYear(), hoy.getMonth(), 1));
}
