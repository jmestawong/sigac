export interface RegistroAuditoria {
  id: number;
  modulo: string;
  accion: string;
  referencia: string | null;
  detalle: string | null;
  monto: number | null;
  usuario: string;
  fecha: string;
}
