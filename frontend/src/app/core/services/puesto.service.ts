import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Puesto, PuestoRequest } from '../models/puesto.model';

@Injectable({ providedIn: 'root' })
export class PuestoService {
  private readonly baseUrl = `${environment.apiUrl}/puestos`;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<Puesto[]> {
    return this.http.get<Puesto[]>(this.baseUrl);
  }

  obtener(id: number): Observable<Puesto> {
    return this.http.get<Puesto>(`${this.baseUrl}/${id}`);
  }

  crear(puesto: PuestoRequest): Observable<Puesto> {
    return this.http.post<Puesto>(this.baseUrl, puesto);
  }

  actualizar(id: number, puesto: PuestoRequest): Observable<Puesto> {
    return this.http.put<Puesto>(`${this.baseUrl}/${id}`, puesto);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
