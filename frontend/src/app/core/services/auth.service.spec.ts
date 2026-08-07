import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { LoginResponse } from '../models/auth.model';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const respuesta: LoginResponse = {
    token: 'token-de-prueba',
    tipo: 'Bearer',
    username: 'admin',
    rol: 'ADMIN',
  };

  beforeEach(() => {
    sessionStorage.clear();

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('inicia sin sesión activa cuando sessionStorage está vacío', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.username()).toBeNull();
    expect(service.rol()).toBeNull();
  });

  it('login exitoso guarda el token y actualiza el estado de sesión', () => {
    service.login({ username: 'admin', password: 'admin123' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush(respuesta);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.username()).toBe('admin');
    expect(service.rol()).toBe('ADMIN');
    expect(service.getToken()).toBe('token-de-prueba');
    expect(sessionStorage.getItem('sigac_token')).toBe('token-de-prueba');
  });

  it('logout limpia el token y el estado de sesión', () => {
    service.login({ username: 'admin', password: 'admin123' }).subscribe();
    httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush(respuesta);

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.username()).toBeNull();
    expect(service.getToken()).toBeNull();
    expect(sessionStorage.getItem('sigac_token')).toBeNull();
  });
});
