import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Giro, GiroRequest } from '../models/giro.model';

@Injectable({ providedIn: 'root' })
export class GiroService {
  private readonly baseUrl = `${environment.apiUrl}/giros`;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<Giro[]> {
    return this.http.get<Giro[]>(this.baseUrl);
  }

  obtener(id: number): Observable<Giro> {
    return this.http.get<Giro>(`${this.baseUrl}/${id}`);
  }

  crear(giro: GiroRequest): Observable<Giro> {
    return this.http.post<Giro>(this.baseUrl, giro);
  }

  actualizar(id: number, giro: GiroRequest): Observable<Giro> {
    return this.http.put<Giro>(`${this.baseUrl}/${id}`, giro);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
