import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CanjeBancarioRequest,
  IngresoExternoRequest,
  ProcesarPagoRequest,
  ProcesarPagoResponse,
  Recibo,
} from '../models/recibo.model';

@Injectable({ providedIn: 'root' })
export class PagoService {
  private readonly baseUrl = `${environment.apiUrl}/pagos`;
  private readonly recibosUrl = `${environment.apiUrl}/recibos`;

  constructor(private readonly http: HttpClient) {}

  procesarPagoCuentas(request: ProcesarPagoRequest): Observable<ProcesarPagoResponse> {
    return this.http.post<ProcesarPagoResponse>(`${this.baseUrl}/procesar-cuentas`, request);
  }

  canjeBancario(request: CanjeBancarioRequest): Observable<Recibo> {
    return this.http.post<Recibo>(`${this.baseUrl}/canje-bancario`, request);
  }

  registrarIngresoExterno(request: IngresoExternoRequest): Observable<Recibo> {
    return this.http.post<Recibo>(`${this.baseUrl}/ingreso-externo`, request);
  }

  listarRecibos(): Observable<Recibo[]> {
    return this.http.get<Recibo[]>(this.recibosUrl);
  }
}
