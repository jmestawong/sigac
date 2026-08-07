import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { Socio } from '../../core/models/socio.model';
import { NotificationService } from '../../core/services/notification.service';
import { SocioService } from '../../core/services/socio.service';
import { SocioList } from './socio-list';

describe('SocioList', () => {
  let socioService: { listar: ReturnType<typeof vi.fn>; eliminar: ReturnType<typeof vi.fn> };
  let notificationService: { exito: ReturnType<typeof vi.fn> };
  let confirmSpy: ReturnType<typeof vi.spyOn>;

  const socio: Socio = {
    id: 1,
    codigo: 'S-001',
    nombres: 'Juan',
    apellidos: 'Pérez',
    accion: 'Ordinaria',
    etapa: 'Activo',
    fechaNacimiento: '1990-05-20',
  };

  function crearComponente() {
    const fixture = TestBed.createComponent(SocioList);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    socioService = { listar: vi.fn().mockReturnValue(of([socio])), eliminar: vi.fn().mockReturnValue(of(undefined)) };
    notificationService = { exito: vi.fn() };
    confirmSpy = vi.spyOn(window, 'confirm');

    TestBed.configureTestingModule({
      imports: [SocioList],
      providers: [
        provideRouter([]),
        { provide: SocioService, useValue: socioService },
        { provide: NotificationService, useValue: notificationService },
        { provide: AuthService, useValue: { username: () => 'admin', rol: () => 'ADMIN' } },
      ],
    });
  });

  it('carga la lista de socios al iniciar', () => {
    const fixture = crearComponente();

    expect(socioService.listar).toHaveBeenCalled();
    expect(fixture.componentInstance.socios()).toEqual([socio]);
    expect(fixture.componentInstance.cargando()).toBe(false);
  });

  it('no elimina si el usuario cancela la confirmación', () => {
    confirmSpy.mockReturnValue(false);
    const fixture = crearComponente();

    fixture.componentInstance.eliminar(socio);

    expect(socioService.eliminar).not.toHaveBeenCalled();
    expect(fixture.componentInstance.socios()).toEqual([socio]);
  });

  it('elimina el socio y actualiza la lista si el usuario confirma', () => {
    confirmSpy.mockReturnValue(true);
    const fixture = crearComponente();

    fixture.componentInstance.eliminar(socio);

    expect(socioService.eliminar).toHaveBeenCalledWith(1);
    expect(fixture.componentInstance.socios()).toEqual([]);
    expect(notificationService.exito).toHaveBeenCalled();
  });
});
