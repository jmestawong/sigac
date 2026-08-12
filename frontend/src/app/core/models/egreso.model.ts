export type EstadoEgreso = 'REGISTRADO' | 'PROCESADO' | 'ANULADO';

export interface Egreso {
  id: number;
  correlativo: string;
  numeroDocumento: string;
  proveedor: string;
  fecha: string;
  importe: number;
  documentoAsociado: string | null;
  motivo: string;
  estado: EstadoEgreso;
}

export interface EgresoRequest {
  numeroDocumento: string;
  proveedor: string;
  fecha: string;
  importe: number;
  documentoAsociado: string | null;
  motivo: string;
}

export interface CargaMasivaEgresosResponse {
  totalFilas: number;
  creados: Egreso[];
  errores: string[];
}
