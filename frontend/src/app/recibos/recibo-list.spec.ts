import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { CuentaPorCobrar } from '../core/models/cuenta-por-cobrar.model';
import { Recibo } from '../core/models/recibo.model';
import { PagoService } from '../core/services/pago.service';
import { ReciboList, destinatarioDeRecibo } from './recibo-list';

function reciboBase(sobrescribir: Partial<Recibo>): Recibo {
  return {
    id: 1,
    correlativo: 'R-000001',
    tipo: 'PAGO_CUENTAS',
    fecha: '2026-10-20T06:46:50',
    monto: 50,
    banco: null,
    fechaDeposito: null,
    depositante: null,
    categoria: null,
    concepto: null,
    cuentas: [],
    ...sobrescribir,
  };
}

const cuentaDePuesto = {
  puesto: { numero: 'P-01', nombreInquilino: 'Carlos Ramos' },
  socio: null,
} as unknown as CuentaPorCobrar;

const cuentaDeSocio = {
  puesto: null,
  socio: { nombres: 'Ana', apellidos: 'Torres' },
} as unknown as CuentaPorCobrar;

describe('destinatarioDeRecibo', () => {
  it('para un ingreso externo muestra al depositante', () => {
    const recibo = reciboBase({ tipo: 'INGRESO_EXTERNO', depositante: 'Municipalidad' });

    expect(destinatarioDeRecibo(recibo)).toBe('Municipalidad');
  });

  it('para un pago de cuentas muestra el puesto o el socio de las cuentas, sin repetir', () => {
    const recibo = reciboBase({ cuentas: [cuentaDePuesto, cuentaDePuesto, cuentaDeSocio] });

    expect(destinatarioDeRecibo(recibo)).toBe('P-01 — Carlos Ramos, Ana Torres');
  });

  it('devuelve vacio cuando el recibo no tiene cuentas asociadas', () => {
    expect(destinatarioDeRecibo(reciboBase({}))).toBe('');
  });
});

describe('ReciboList', () => {
  let pagoService: { listarRecibos: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    pagoService = { listarRecibos: vi.fn().mockReturnValue(of([reciboBase({ cuentas: [cuentaDePuesto] })])) };

    TestBed.configureTestingModule({
      imports: [ReciboList],
      providers: [provideRouter([]), { provide: PagoService, useValue: pagoService }],
    });
  });

  it('carga los recibos al iniciar, sin filtros', () => {
    const fixture = TestBed.createComponent(ReciboList);
    fixture.detectChanges();

    expect(pagoService.listarRecibos).toHaveBeenCalledWith(undefined, undefined);
    expect(fixture.componentInstance.recibos()).toHaveLength(1);
  });

  it('al cambiar la fecha o el tipo consulta al backend con ese filtro (RF-29, RF-31)', () => {
    const fixture = TestBed.createComponent(ReciboList);
    fixture.detectChanges();

    fixture.componentInstance.cambiarFecha('2026-10-20');
    expect(pagoService.listarRecibos).toHaveBeenLastCalledWith('2026-10-20', undefined);

    fixture.componentInstance.cambiarTipo('CANJE_BANCARIO');
    expect(pagoService.listarRecibos).toHaveBeenLastCalledWith('2026-10-20', 'CANJE_BANCARIO');
  });

  it('la busqueda de texto tambien encuentra por destinatario', () => {
    const fixture = TestBed.createComponent(ReciboList);
    fixture.detectChanges();

    fixture.componentInstance.filtro.set('carlos');
    expect(fixture.componentInstance.recibosFiltrados()).toHaveLength(1);

    fixture.componentInstance.filtro.set('inexistente');
    expect(fixture.componentInstance.recibosFiltrados()).toHaveLength(0);
  });
});
