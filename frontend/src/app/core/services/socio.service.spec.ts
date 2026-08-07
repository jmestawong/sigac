import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { Socio, SocioRequest } from '../models/socio.model';
import { SocioService } from './socio.service';

describe('SocioService', () => {
  let service: SocioService;
  let httpMock: HttpTestingController;

  const baseUrl = `${environment.apiUrl}/socios`;

  const socio: Socio = {
    id: 1,
    codigo: 'S-001',
    nombres: 'Juan',
    apellidos: 'Pérez',
    accion: 'Ordinaria',
    etapa: 'Activo',
    fechaNacimiento: '1990-05-20',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(SocioService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listar hace GET a /socios y retorna la lista', () => {
    let resultado: Socio[] | undefined;
    service.listar().subscribe((socios) => (resultado = socios));

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush([socio]);

    expect(resultado).toEqual([socio]);
  });

  it('obtener hace GET a /socios/{id}', () => {
    service.obtener(1).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(socio);
  });

  it('crear hace POST a /socios con el cuerpo del request', () => {
    const request: SocioRequest = {
      codigo: 'S-001',
      nombres: 'Juan',
      apellidos: 'Pérez',
      accion: 'Ordinaria',
      etapa: 'Activo',
      fechaNacimiento: '1990-05-20',
    };

    let resultado: Socio | undefined;
    service.crear(request).subscribe((s) => (resultado = s));

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(socio);

    expect(resultado).toEqual(socio);
  });

  it('actualizar hace PUT a /socios/{id} con el cuerpo del request', () => {
    const request: SocioRequest = { ...socio, etapa: 'Suspendido' };
    delete (request as Partial<Socio>).id;

    service.actualizar(1, request).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush({ ...socio, etapa: 'Suspendido' });
  });

  it('eliminar hace DELETE a /socios/{id}', () => {
    service.eliminar(1).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
