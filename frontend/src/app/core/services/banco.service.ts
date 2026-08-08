import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Banco, BancoRequest } from '../models/banco.model';

@Injectable({ providedIn: 'root' })
export class BancoService {
  private readonly baseUrl = `${environment.apiUrl}/bancos`;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<Banco[]> {
    return this.http.get<Banco[]>(this.baseUrl);
  }

  obtener(id: number): Observable<Banco> {
    return this.http.get<Banco>(`${this.baseUrl}/${id}`);
  }

  crear(banco: BancoRequest): Observable<Banco> {
    return this.http.post<Banco>(this.baseUrl, banco);
  }

  actualizar(id: number, banco: BancoRequest): Observable<Banco> {
    return this.http.put<Banco>(`${this.baseUrl}/${id}`, banco);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
