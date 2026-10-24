const dosDigitos = (n: number): string => String(n).padStart(2, '0');

export function crearFechaLocal(anio: number, mes: number, dia: number): Date | null {
  const fecha = new Date(anio, mes - 1, dia);
  const valida =
    fecha.getFullYear() === anio && fecha.getMonth() === mes - 1 && fecha.getDate() === dia;
  return valida ? fecha : null;
}

export function desdeIsoLocal(valor: string): Date | null {
  const coincidencia = /^(\d{4})-(\d{2})-(\d{2})$/.exec(valor);
  return coincidencia
    ? crearFechaLocal(Number(coincidencia[1]), Number(coincidencia[2]), Number(coincidencia[3]))
    : null;
}

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

export function hoyIsoLocal(): string {
  return aIsoLocal(new Date());
}

export function mesActualIsoLocal(): string {
  return hoyIsoLocal().slice(0, 7);
}

export function primerDiaDelMesIsoLocal(): string {
  const hoy = new Date();
  return aIsoLocal(new Date(hoy.getFullYear(), hoy.getMonth(), 1));
}
