import { Giro } from './giro.model';
import { Socio } from './socio.model';

export interface Puesto {
  id: number;
  numero: string;
  nombreInquilino: string;
  fechaInicioVigencia: string;
  fechaFinVigencia: string;
  giro: Giro;
  socio: Socio | null;
}

export interface PuestoRequest {
  numero: string;
  nombreInquilino: string;
  fechaInicioVigencia: string;
  fechaFinVigencia: string;
  giroId: number;
  socioId: number | null;
}
