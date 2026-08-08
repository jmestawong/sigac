import { Puesto } from './puesto.model';
import { ServicioCobrable } from './servicio-cobrable.model';
import { Socio } from './socio.model';

export type EstadoCuenta = 'PENDIENTE' | 'ABONADA' | 'EXONERADA';

export interface CuentaPorCobrar {
  id: number;
  estado: EstadoCuenta;
  monto: number;
  periodo: string;
  puesto: Puesto | null;
  socio: Socio | null;
  servicio: ServicioCobrable;
  lecturaInicial: number | null;
  lecturaFinal: number | null;
}

export interface GenerarPuestosMontoFijoRequest {
  servicioId: number;
  periodo: string;
  monto: number;
  puestoIds: number[];
}

export interface LecturaPuestoRequest {
  puestoId: number;
  lecturaInicial: number;
  lecturaFinal: number;
}

export interface GenerarPuestosConsumoRequest {
  servicioId: number;
  periodo: string;
  lecturas: LecturaPuestoRequest[];
}

export interface GenerarSociosRequest {
  servicioId: number;
  periodo: string;
  monto: number;
  etapas: string[];
  sociosUnicos: boolean;
}
