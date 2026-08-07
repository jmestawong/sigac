export interface Socio {
  id: number;
  codigo: string;
  nombres: string;
  apellidos: string;
  accion: string;
  etapa: string;
  fechaNacimiento: string;
}

export type SocioRequest = Omit<Socio, 'id'>;
