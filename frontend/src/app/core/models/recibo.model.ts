import { Banco } from './banco.model';
import { CuentaPorCobrar } from './cuenta-por-cobrar.model';

export type TipoRecibo = 'PAGO_CUENTAS' | 'CANJE_BANCARIO' | 'INGRESO_EXTERNO';

export interface Recibo {
  id: number;
  correlativo: string;
  tipo: TipoRecibo;
  fecha: string;
  monto: number;
  banco: Banco | null;
  fechaDeposito: string | null;
  depositante: string | null;
  categoria: string | null;
  concepto: string | null;
  cuentas: CuentaPorCobrar[];
}

export interface ProcesarPagoRequest {
  cuentasAbonadasIds: number[];
  cuentasExoneradasIds: number[];
}

export interface ProcesarPagoResponse {
  recibo: Recibo | null;
  cuentasActualizadas: CuentaPorCobrar[];
}

export interface CanjeBancarioRequest {
  cuentaId: number;
  bancoId: number;
  fechaDeposito: string;
}

export interface IngresoExternoRequest {
  depositante: string;
  categoria: string;
  concepto: string;
  monto: number;
}
