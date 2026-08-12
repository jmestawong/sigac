import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

export type TipoReporte = 'movimientos' | 'totales' | 'socios' | 'no-socios' | 'egresos' | 'bancos';

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private readonly baseUrl = `${environment.apiUrl}/reportes`;

  constructor(private readonly http: HttpClient) {}

  descargar(tipo: TipoReporte, desde: string, hasta: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${tipo}`, {
      params: { desde, hasta },
      responseType: 'blob',
    });
  }

  guardarArchivo(blob: Blob, tipo: TipoReporte, desde: string, hasta: string): void {
    const url = window.URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = `reporte-${tipo}-${desde}_a_${hasta}.xlsx`;
    enlace.click();
    window.URL.revokeObjectURL(url);
  }
}
