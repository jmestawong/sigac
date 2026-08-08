import { Moneda } from './banco.model';

export type Recurrencia = 'MENSUAL' | 'BIMESTRAL' | 'TRIMESTRAL' | 'ANUAL' | 'UNICA';
export type TipoDestinatario = 'PUESTO' | 'SOCIO';

export interface ServicioCobrable {
  id: number;
  nombre: string;
  recurrencia: Recurrencia;
  costo: number;
  moneda: Moneda;
  destinatario: TipoDestinatario;
  esPorConsumo: boolean;
}

export type ServicioCobrableRequest = Omit<ServicioCobrable, 'id'>;
