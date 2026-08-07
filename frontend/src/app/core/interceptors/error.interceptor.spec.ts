import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';
import { errorInterceptor } from './error.interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: { logout: ReturnType<typeof vi.fn> };
  let notificationService: { error: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(() => {
    authService = { logout: vi.fn() };
    notificationService = { error: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: NotificationService, useValue: notificationService },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => httpMock.verify());

  it('en un 401 fuera del login: cierra sesión, redirige y notifica', () => {
    http.get('/api/socios').subscribe({ error: () => {} });

    httpMock.expectOne('/api/socios').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    expect(notificationService.error).toHaveBeenCalled();
  });

  it('en un 401 del propio login no cierra sesión ni redirige', () => {
    http.post('/api/auth/login', {}).subscribe({ error: () => {} });

    httpMock.expectOne('/api/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
    expect(notificationService.error).toHaveBeenCalled();
  });

  it('en otros errores muestra el mensaje del backend sin cerrar sesión', () => {
    http.post('/api/socios', {}).subscribe({ error: () => {} });

    httpMock
      .expectOne('/api/socios')
      .flush({ message: 'El codigo ya existe' }, { status: 409, statusText: 'Conflict' });

    expect(authService.logout).not.toHaveBeenCalled();
    expect(notificationService.error).toHaveBeenCalledWith('El codigo ya existe');
  });
});
