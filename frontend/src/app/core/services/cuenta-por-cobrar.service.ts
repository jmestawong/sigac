import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CuentaPorCobrar,
  GenerarPuestosConsumoRequest,
  GenerarPuestosMontoFijoRequest,
  GenerarSociosRequest,
} from '../models/cuenta-por-cobrar.model';

@Injectable({ providedIn: 'root' })
export class CuentaPorCobrarService {
  private readonly baseUrl = `${environment.apiUrl}/cuentas-por-cobrar`;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<CuentaPorCobrar[]> {
    return this.http.get<CuentaPorCobrar[]>(this.baseUrl);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  generarParaPuestosMontoFijo(request: GenerarPuestosMontoFijoRequest): Observable<CuentaPorCobrar[]> {
    return this.http.post<CuentaPorCobrar[]>(`${this.baseUrl}/generar/puestos-monto-fijo`, request);
  }

  generarParaPuestosConsumo(request: GenerarPuestosConsumoRequest): Observable<CuentaPorCobrar[]> {
    return this.http.post<CuentaPorCobrar[]>(`${this.baseUrl}/generar/puestos-consumo`, request);
  }

  generarParaSocios(request: GenerarSociosRequest): Observable<CuentaPorCobrar[]> {
    return this.http.post<CuentaPorCobrar[]>(`${this.baseUrl}/generar/socios`, request);
  }
}
