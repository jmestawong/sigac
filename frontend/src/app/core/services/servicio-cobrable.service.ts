import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ServicioCobrable, ServicioCobrableRequest } from '../models/servicio-cobrable.model';

@Injectable({ providedIn: 'root' })
export class ServicioCobrableService {
  private readonly baseUrl = `${environment.apiUrl}/servicios`;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<ServicioCobrable[]> {
    return this.http.get<ServicioCobrable[]>(this.baseUrl);
  }

  obtener(id: number): Observable<ServicioCobrable> {
    return this.http.get<ServicioCobrable>(`${this.baseUrl}/${id}`);
  }

  crear(servicio: ServicioCobrableRequest): Observable<ServicioCobrable> {
    return this.http.post<ServicioCobrable>(this.baseUrl, servicio);
  }

  actualizar(id: number, servicio: ServicioCobrableRequest): Observable<ServicioCobrable> {
    return this.http.put<ServicioCobrable>(`${this.baseUrl}/${id}`, servicio);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
