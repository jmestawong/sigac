import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const notificationService = inject(NotificationService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/auth/login')) {
        authService.logout();
        router.navigate(['/login']);
        notificationService.error('Tu sesion expiro. Vuelve a iniciar sesion.');
      } else {
        const mensaje = error.error?.message ?? 'Ocurrio un error inesperado. Intenta nuevamente.';
        notificationService.error(mensaje);
      }

      return throwError(() => error);
    }),
  );
};
