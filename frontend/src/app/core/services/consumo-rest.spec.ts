import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuditoriaService } from './auditoria.service';
import { BancoService } from './banco.service';
import { CuentaPorCobrarService } from './cuenta-por-cobrar.service';
import { EgresoService } from './egreso.service';
import { GiroService } from './giro.service';
import { PagoService } from './pago.service';
import { PuestoService } from './puesto.service';
import { ReporteService, TipoReporte } from './reporte.service';
import { ServicioCobrableService } from './servicio-cobrable.service';
import { SocioService } from './socio.service';

/**
 * Verifica que la aplicacion Angular consume los servicios REST del backend con
 * los metodos HTTP GET, POST, PUT y DELETE correctos, en cada servicio.
 */
describe('Consumo de servicios REST desde Angular', () => {
  let httpMock: HttpTestingController;
  const api = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  /** Dispara la llamada, comprueba metodo + URL (+ cuerpo) y responde para completar el Observable. */
  function verificar(
    llamada: Observable<unknown>,
    metodo: 'GET' | 'POST' | 'PUT' | 'DELETE',
    url: string,
    cuerpo?: unknown,
  ): void {
    llamada.subscribe();
    const req = httpMock.expectOne({ method: metodo, url });
    if (cuerpo !== undefined) {
      expect(req.request.body).toEqual(cuerpo);
    }
    req.flush(null);
  }

  interface Crud {
    listar(): Observable<unknown>;
    obtener(id: number): Observable<unknown>;
    crear(datos: never): Observable<unknown>;
    actualizar(id: number, datos: never): Observable<unknown>;
    eliminar(id: number): Observable<unknown>;
  }

  const catalogos: { nombre: string; ruta: string; servicio: () => Crud }[] = [
    { nombre: 'SocioService', ruta: 'socios', servicio: () => TestBed.inject(SocioService) as Crud },
    { nombre: 'GiroService', ruta: 'giros', servicio: () => TestBed.inject(GiroService) as Crud },
    { nombre: 'BancoService', ruta: 'bancos', servicio: () => TestBed.inject(BancoService) as Crud },
    { nombre: 'PuestoService', ruta: 'puestos', servicio: () => TestBed.inject(PuestoService) as Crud },
    {
      nombre: 'ServicioCobrableService',
      ruta: 'servicios',
      servicio: () => TestBed.inject(ServicioCobrableService) as Crud,
    },
  ];

  for (const { nombre, ruta, servicio } of catalogos) {
    describe(`${nombre} (/api/${ruta})`, () => {
      const datos = { campo: 'valor' } as never;

      it('listar -> GET', () => verificar(servicio().listar(), 'GET', `${api}/${ruta}`));
      it('obtener -> GET por id', () => verificar(servicio().obtener(7), 'GET', `${api}/${ruta}/7`));
      it('crear -> POST', () => verificar(servicio().crear(datos), 'POST', `${api}/${ruta}`, datos));
      it('actualizar -> PUT por id', () =>
        verificar(servicio().actualizar(7, datos), 'PUT', `${api}/${ruta}/7`, datos));
      it('eliminar -> DELETE por id', () => verificar(servicio().eliminar(7), 'DELETE', `${api}/${ruta}/7`));
    });
  }

  describe('CuentaPorCobrarService (/api/cuentas-por-cobrar)', () => {
    const base = `${api}/cuentas-por-cobrar`;

    it('listar -> GET', () => verificar(TestBed.inject(CuentaPorCobrarService).listar(), 'GET', base));
    it('obtener -> GET por id', () =>
      verificar(TestBed.inject(CuentaPorCobrarService).obtener(3), 'GET', `${base}/3`));
    it('eliminar -> DELETE por id', () =>
      verificar(TestBed.inject(CuentaPorCobrarService).eliminar(3), 'DELETE', `${base}/3`));
    it('generar para puestos (monto fijo) -> POST', () =>
      verificar(
        TestBed.inject(CuentaPorCobrarService).generarParaPuestosMontoFijo({
          servicioId: 1,
          periodo: '2026-10',
          monto: 50,
          puestoIds: [1],
        }),
        'POST',
        `${base}/generar/puestos-monto-fijo`,
      ));
    it('generar para puestos (consumo) -> POST', () =>
      verificar(
        TestBed.inject(CuentaPorCobrarService).generarParaPuestosConsumo({
          servicioId: 2,
          periodo: '2026-10',
          lecturas: [],
        }),
        'POST',
        `${base}/generar/puestos-consumo`,
      ));
    it('generar para socios -> POST', () =>
      verificar(
        TestBed.inject(CuentaPorCobrarService).generarParaSocios({
          servicioId: 3,
          periodo: '2027',
          monto: 100,
          etapas: ['1'],
          sociosUnicos: true,
        }),
        'POST',
        `${base}/generar/socios`,
      ));
    it('resumen por socio y por puesto -> GET', () => {
      verificar(TestBed.inject(CuentaPorCobrarService).resumenPorSocio(4), 'GET', `${base}/resumen/socio/4`);
      verificar(TestBed.inject(CuentaPorCobrarService).resumenPorPuesto(5), 'GET', `${base}/resumen/puesto/5`);
    });
    it('referencias (uso en cobranza) -> GET', () =>
      verificar(TestBed.inject(CuentaPorCobrarService).referencias('socio', 8), 'GET', `${base}/referencias/socio/8`));
  });

  describe('PagoService (/api/pagos y /api/recibos)', () => {
    it('procesar pago de cuentas -> POST', () =>
      verificar(
        TestBed.inject(PagoService).procesarPagoCuentas({ cuentasAbonadasIds: [1], cuentasExoneradasIds: [] }),
        'POST',
        `${api}/pagos/procesar-cuentas`,
      ));
    it('canje bancario -> POST', () =>
      verificar(
        TestBed.inject(PagoService).canjeBancario({ cuentaId: 1, bancoId: 1, fechaDeposito: '2026-10-01' }),
        'POST',
        `${api}/pagos/canje-bancario`,
      ));
    it('ingreso externo -> POST', () =>
      verificar(
        TestBed.inject(PagoService).registrarIngresoExterno({
          depositante: 'X',
          categoria: 'Donacion',
          concepto: 'Aporte',
          monto: 10,
        }),
        'POST',
        `${api}/pagos/ingreso-externo`,
      ));
    it('listar recibos -> GET (con filtros opcionales de fecha y tipo)', () => {
      TestBed.inject(PagoService).listarRecibos('2026-10-20', 'PAGO_CUENTAS').subscribe();
      const req = httpMock.expectOne(
        (r) =>
          r.method === 'GET' &&
          r.url === `${api}/recibos` &&
          r.params.get('fecha') === '2026-10-20' &&
          r.params.get('tipo') === 'PAGO_CUENTAS',
      );
      req.flush([]);
    });
    it('obtener recibo -> GET por id', () =>
      verificar(TestBed.inject(PagoService).obtenerRecibo(9), 'GET', `${api}/recibos/9`));
  });

  describe('EgresoService (/api/egresos)', () => {
    const base = `${api}/egresos`;

    it('listar -> GET con rango de fechas', () => {
      TestBed.inject(EgresoService).listar('2026-10-01', '2026-10-31').subscribe();
      const req = httpMock.expectOne(
        (r) =>
          r.method === 'GET' &&
          r.url === base &&
          r.params.get('desde') === '2026-10-01' &&
          r.params.get('hasta') === '2026-10-31',
      );
      req.flush([]);
    });
    it('obtener -> GET por id', () => verificar(TestBed.inject(EgresoService).obtener(2), 'GET', `${base}/2`));
    it('registrar -> POST', () =>
      verificar(
        TestBed.inject(EgresoService).registrar({
          numeroDocumento: 'F-1',
          proveedor: 'Proveedor',
          fecha: '2026-10-20',
          importe: 10,
          documentoAsociado: null,
          motivo: 'Compra',
        }),
        'POST',
        base,
      ));
    it('procesar y anular -> POST', () => {
      verificar(TestBed.inject(EgresoService).procesar(2), 'POST', `${base}/2/procesar`);
      verificar(TestBed.inject(EgresoService).anular(2), 'POST', `${base}/2/anular`);
    });
    it('carga masiva -> POST multipart con el archivo', () => {
      const archivo = new File(['x'], 'egresos.xlsx');
      TestBed.inject(EgresoService).registrarMasivo(archivo).subscribe();
      const req = httpMock.expectOne({ method: 'POST', url: `${base}/carga-masiva` });
      expect(req.request.body instanceof FormData).toBe(true);
      expect((req.request.body as FormData).get('archivo')).toBeInstanceOf(File);
      req.flush(null);
    });
  });

  describe('ReporteService (/api/reportes)', () => {
    const tipos: TipoReporte[] = ['movimientos', 'totales', 'socios', 'no-socios', 'egresos', 'bancos'];

    for (const tipo of tipos) {
      it(`descargar ${tipo} -> GET como archivo (blob)`, () => {
        TestBed.inject(ReporteService).descargar(tipo, '2026-10-01', '2026-10-31').subscribe();
        const req = httpMock.expectOne(
          (r) =>
            r.method === 'GET' &&
            r.url === `${api}/reportes/${tipo}` &&
            r.params.get('desde') === '2026-10-01' &&
            r.params.get('hasta') === '2026-10-31',
        );
        expect(req.request.responseType).toBe('blob');
        req.flush(new Blob(['xlsx']));
      });
    }
  });
  describe('AuditoriaService (/api/auditoria)', () => {
    it('listar -> GET con filtros opcionales de modulo y usuario', () => {
      TestBed.inject(AuditoriaService).listar('COBRANZA', 'operador').subscribe();
      const req = httpMock.expectOne(
        (r) =>
          r.method === 'GET' &&
          r.url === `${api}/auditoria` &&
          r.params.get('modulo') === 'COBRANZA' &&
          r.params.get('usuario') === 'operador',
      );
      req.flush([]);
    });

    it('listar sin filtros -> GET sin parametros', () => {
      TestBed.inject(AuditoriaService).listar().subscribe();
      const req = httpMock.expectOne((r) => r.method === 'GET' && r.url === `${api}/auditoria`);
      expect(req.request.params.keys()).toHaveLength(0);
      req.flush([]);
    });
  });
});
