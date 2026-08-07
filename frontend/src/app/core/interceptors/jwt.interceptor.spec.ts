import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { jwtInterceptor } from './jwt.interceptor';

describe('jwtInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  function configurar(): void {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([jwtInterceptor])), provideHttpClientTesting()],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  }

  afterEach(() => {
    sessionStorage.clear();
    httpMock.verify();
  });

  it('agrega el header Authorization cuando hay un token', () => {
    sessionStorage.setItem('sigac_token', 'token-de-prueba');
    configurar();

    http.get('/api/socios').subscribe();

    const req = httpMock.expectOne('/api/socios');
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-de-prueba');
    req.flush([]);
  });

  it('no agrega el header en la petición de login aunque haya un token', () => {
    sessionStorage.setItem('sigac_token', 'token-de-prueba');
    configurar();

    http.post('/api/auth/login', {}).subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('no agrega el header cuando no hay token', () => {
    configurar();

    http.get('/api/socios').subscribe();

    const req = httpMock.expectOne('/api/socios');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });
});
