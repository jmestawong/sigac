export type Moneda = 'PEN' | 'USD';

export interface Banco {
  id: number;
  nombre: string;
  numeroCuenta: string;
  cci: string;
  moneda: Moneda;
}

export type BancoRequest = Omit<Banco, 'id'>;
