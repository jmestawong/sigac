import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { adminGuard } from './admin.guard';

describe('adminGuard', () => {
  let authService: { rol: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(() => {
    authService = { rol: vi.fn() };

    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    });

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('permite el acceso cuando el rol es ADMIN', () => {
    authService.rol.mockReturnValue('ADMIN');

    const resultado = TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never));

    expect(resultado).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('bloquea el acceso y redirige a /cobranza cuando el rol es OPERADOR', () => {
    authService.rol.mockReturnValue('OPERADOR');

    const resultado = TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never));

    expect(resultado).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/cobranza']);
  });
});
