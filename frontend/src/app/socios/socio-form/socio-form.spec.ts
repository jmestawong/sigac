import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { Socio, SocioRequest } from '../../core/models/socio.model';
import { NotificationService } from '../../core/services/notification.service';
import { SocioService } from '../../core/services/socio.service';
import { SocioForm } from './socio-form';

describe('SocioForm', () => {
  let socioService: {
    obtener: ReturnType<typeof vi.fn>;
    crear: ReturnType<typeof vi.fn>;
    actualizar: ReturnType<typeof vi.fn>;
  };
  let notificationService: { exito: ReturnType<typeof vi.fn> };
  let router: Router;

  const socio: Socio = {
    id: 1,
    codigo: 'S-001',
    nombres: 'Juan',
    apellidos: 'Pérez',
    accion: 'Ordinaria',
    etapa: 'Activo',
    fechaNacimiento: '1990-05-20',
  };

  const solicitud: SocioRequest = {
    codigo: 'S-001',
    nombres: 'Juan',
    apellidos: 'Pérez',
    accion: 'Ordinaria',
    etapa: 'Activo',
    fechaNacimiento: '1990-05-20',
  };

  function configurar(idRuta: string | null) {
    socioService = {
      obtener: vi.fn().mockReturnValue(of(socio)),
      crear: vi.fn().mockReturnValue(of(socio)),
      actualizar: vi.fn().mockReturnValue(of(socio)),
    };
    notificationService = { exito: vi.fn() };

    TestBed.configureTestingModule({
      imports: [SocioForm],
      providers: [
        provideRouter([]),
        { provide: SocioService, useValue: socioService },
        { provide: NotificationService, useValue: notificationService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => idRuta } } },
        },
      ],
    });

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const fixture = TestBed.createComponent(SocioForm);
    fixture.detectChanges();
    return fixture;
  }

  it('modo creación: no llama a crear si el formulario es inválido', () => {
    const fixture = configurar(null);

    fixture.componentInstance.submit();

    expect(fixture.componentInstance.esEdicion()).toBe(false);
    expect(socioService.crear).not.toHaveBeenCalled();
  });

  it('modo creación: envía el formulario y navega a /socios', () => {
    const fixture = configurar(null);

    fixture.componentInstance.form.setValue(solicitud);
    fixture.componentInstance.submit();

    expect(socioService.crear).toHaveBeenCalledWith(solicitud);
    expect(notificationService.exito).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/socios']);
  });

  it('modo edición: precarga los datos del socio existente', () => {
    const fixture = configurar('1');

    expect(socioService.obtener).toHaveBeenCalledWith(1);
    expect(fixture.componentInstance.esEdicion()).toBe(true);
    expect(fixture.componentInstance.form.getRawValue()).toEqual(solicitud);
  });

  it('modo edición: envía los cambios con actualizar', () => {
    const fixture = configurar('1');

    fixture.componentInstance.form.patchValue({ etapa: 'Suspendido' });
    fixture.componentInstance.submit();

    expect(socioService.actualizar).toHaveBeenCalledWith(1, { ...solicitud, etapa: 'Suspendido' });
    expect(router.navigate).toHaveBeenCalledWith(['/socios']);
  });
});
