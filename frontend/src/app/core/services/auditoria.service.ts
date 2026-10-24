import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RegistroAuditoria } from '../models/auditoria.model';

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auditoria`;

  /** Bitacora de movimientos que ms-auditoria recibe por RabbitMQ (solo ADMIN). */
  listar(modulo?: string, usuario?: string): Observable<RegistroAuditoria[]> {
    let params = new HttpParams();
    if (modulo) {
      params = params.set('modulo', modulo);
    }
    if (usuario) {
      params = params.set('usuario', usuario);
    }
    return this.http.get<RegistroAuditoria[]>(this.baseUrl, { params });
  }
}
