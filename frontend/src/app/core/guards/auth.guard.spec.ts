import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let authService: { isAuthenticated: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(() => {
    authService = { isAuthenticated: vi.fn() };

    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    });

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('permite el acceso cuando hay sesión activa', () => {
    authService.isAuthenticated.mockReturnValue(true);

    const resultado = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    expect(resultado).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('bloquea el acceso y redirige a /login cuando no hay sesión', () => {
    authService.isAuthenticated.mockReturnValue(false);

    const resultado = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    expect(resultado).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
