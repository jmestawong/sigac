export interface Giro {
  id: number;
  nombre: string;
}

export type GiroRequest = Omit<Giro, 'id'>;
