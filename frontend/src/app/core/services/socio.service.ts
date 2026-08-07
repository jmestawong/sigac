import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Socio, SocioRequest } from '../models/socio.model';

@Injectable({ providedIn: 'root' })
export class SocioService {
  private readonly baseUrl = `${environment.apiUrl}/socios`;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<Socio[]> {
    return this.http.get<Socio[]>(this.baseUrl);
  }

  obtener(id: number): Observable<Socio> {
    return this.http.get<Socio>(`${this.baseUrl}/${id}`);
  }

  crear(socio: SocioRequest): Observable<Socio> {
    return this.http.post<Socio>(this.baseUrl, socio);
  }

  actualizar(id: number, socio: SocioRequest): Observable<Socio> {
    return this.http.put<Socio>(`${this.baseUrl}/${id}`, socio);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
