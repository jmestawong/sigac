import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CargaMasivaEgresosResponse, Egreso, EgresoRequest } from '../models/egreso.model';

@Injectable({ providedIn: 'root' })
export class EgresoService {
  private readonly baseUrl = `${environment.apiUrl}/egresos`;

  constructor(private readonly http: HttpClient) {}

  listar(desde?: string, hasta?: string): Observable<Egreso[]> {
    let params = new HttpParams();
    if (desde) {
      params = params.set('desde', desde);
    }
    if (hasta) {
      params = params.set('hasta', hasta);
    }
    return this.http.get<Egreso[]>(this.baseUrl, { params });
  }

  obtener(id: number): Observable<Egreso> {
    return this.http.get<Egreso>(`${this.baseUrl}/${id}`);
  }

  registrar(egreso: EgresoRequest): Observable<Egreso> {
    return this.http.post<Egreso>(this.baseUrl, egreso);
  }

  procesar(id: number): Observable<Egreso> {
    return this.http.post<Egreso>(`${this.baseUrl}/${id}/procesar`, {});
  }

  anular(id: number): Observable<Egreso> {
    return this.http.post<Egreso>(`${this.baseUrl}/${id}/anular`, {});
  }

  registrarMasivo(archivo: File): Observable<CargaMasivaEgresosResponse> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.http.post<CargaMasivaEgresosResponse>(`${this.baseUrl}/carga-masiva`, formData);
  }
}
