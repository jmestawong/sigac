import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { Login } from './login';

describe('Login', () => {
  let authService: { login: ReturnType<typeof vi.fn>; rol: ReturnType<typeof vi.fn> };
  let router: Router;

  function crearComponente() {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    authService = { login: vi.fn(), rol: vi.fn().mockReturnValue('ADMIN') };

    TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    });

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('no llama al servicio de login si el formulario está incompleto', () => {
    const fixture = crearComponente();

    fixture.componentInstance.submit();

    expect(authService.login).not.toHaveBeenCalled();
    expect(fixture.componentInstance.form.touched).toBe(true);
  });

  it('navega a /socios cuando el login es exitoso', () => {
    authService.login.mockReturnValue(
      of({ token: 't', tipo: 'Bearer', username: 'admin', rol: 'ADMIN' }),
    );
    const fixture = crearComponente();

    fixture.componentInstance.form.setValue({ username: 'admin', password: 'admin123' });
    fixture.componentInstance.submit();

    expect(authService.login).toHaveBeenCalledWith({ username: 'admin', password: 'admin123' });
    expect(router.navigate).toHaveBeenCalledWith(['/socios']);
    expect(fixture.componentInstance.cargando()).toBe(false);
  });

  it('navega a /cobranza cuando el login es exitoso y el rol es OPERADOR', () => {
    authService.login.mockReturnValue(
      of({ token: 't', tipo: 'Bearer', username: 'operador', rol: 'OPERADOR' }),
    );
    authService.rol.mockReturnValue('OPERADOR');
    const fixture = crearComponente();

    fixture.componentInstance.form.setValue({ username: 'operador', password: 'operador123' });
    fixture.componentInstance.submit();

    expect(router.navigate).toHaveBeenCalledWith(['/cobranza']);
  });

  it('muestra un mensaje de error cuando las credenciales son inválidas', () => {
    authService.login.mockReturnValue(throwError(() => new Error('401')));
    const fixture = crearComponente();

    fixture.componentInstance.form.setValue({ username: 'admin', password: 'incorrecta' });
    fixture.componentInstance.submit();

    expect(fixture.componentInstance.errorLogin()).toBe('Usuario o contraseña incorrectos.');
    expect(fixture.componentInstance.cargando()).toBe(false);
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
