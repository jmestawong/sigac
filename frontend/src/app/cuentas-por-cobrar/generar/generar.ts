import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CuentaPorCobrarService } from '../../core/services/cuenta-por-cobrar.service';
import { NotificationService } from '../../core/services/notification.service';
import { PuestoService } from '../../core/services/puesto.service';
import { ServicioCobrableService } from '../../core/services/servicio-cobrable.service';
import { SocioService } from '../../core/services/socio.service';
import { CuentaPorCobrar, LecturaPuestoRequest } from '../../core/models/cuenta-por-cobrar.model';
import { Puesto } from '../../core/models/puesto.model';
import { ServicioCobrable } from '../../core/models/servicio-cobrable.model';
import { Socio } from '../../core/models/socio.model';

type Modo = 'PUESTO_MONTO_FIJO' | 'PUESTO_CONSUMO' | 'SOCIO';

interface LecturaFila {
  puesto: Puesto;
  lecturaInicial: number | null;
  lecturaFinal: number | null;
}

const ETAPAS_DISPONIBLES = ['1', '2', '3'];

function claveSocio(socio: Socio): string {
  return `${socio.nombres.trim().toLowerCase()}|${socio.apellidos.trim().toLowerCase()}`;
}

function deduplicarSocios(socios: Socio[]): Socio[] {
  const vistos = new Set<string>();
  const resultado: Socio[] = [];
  for (const socio of socios) {
    const clave = claveSocio(socio);
    if (!vistos.has(clave)) {
      vistos.add(clave);
      resultado.push(socio);
    }
  }
  return resultado;
}

@Component({
  selector: 'app-generar-cuentas',
  imports: [RouterLink],
  templateUrl: './generar.html',
  styleUrl: './generar.css',
})
export class Generar {
  private readonly servicioService = inject(ServicioCobrableService);
  private readonly puestoService = inject(PuestoService);
  private readonly socioService = inject(SocioService);
  private readonly cuentaService = inject(CuentaPorCobrarService);
  private readonly notificationService = inject(NotificationService);

  readonly etapasDisponibles = ETAPAS_DISPONIBLES;
  protected readonly Number = Number;

  readonly servicios = signal<ServicioCobrable[]>([]);
  readonly puestos = signal<Puesto[]>([]);
  readonly socios = signal<Socio[]>([]);

  readonly servicioId = signal<number | null>(null);
  readonly periodo = signal('');
  readonly monto = signal<number | null>(null);
  readonly generando = signal(false);
  readonly cuentasGeneradas = signal<CuentaPorCobrar[] | null>(null);

  readonly puestoIdsSeleccionados = signal<Set<number>>(new Set());
  readonly lecturasPorPuesto = signal<Map<number, LecturaFila>>(new Map());
  readonly etapasSeleccionadas = signal<Set<string>>(new Set());
  readonly sociosUnicos = signal(false);

  readonly servicioSeleccionado = computed(() => this.servicios().find((s) => s.id === this.servicioId()) ?? null);

  readonly modo = computed<Modo | null>(() => {
    const servicio = this.servicioSeleccionado();
    if (!servicio) {
      return null;
    }
    if (servicio.destinatario === 'PUESTO') {
      return servicio.esPorConsumo ? 'PUESTO_CONSUMO' : 'PUESTO_MONTO_FIJO';
    }
    return 'SOCIO';
  });

  readonly filasConsumo = computed<LecturaFila[]>(() => {
    const mapa = this.lecturasPorPuesto();
    return this.puestos().map(
      (puesto) => mapa.get(puesto.id) ?? { puesto, lecturaInicial: null, lecturaFinal: null },
    );
  });

  readonly sociosCandidatos = computed(() => {
    let lista = this.socios();
    const etapas = this.etapasSeleccionadas();
    if (etapas.size > 0) {
      lista = lista.filter((socio) => etapas.has(socio.etapa));
    }
    if (this.sociosUnicos()) {
      lista = deduplicarSocios(lista);
    }
    return lista;
  });

  constructor() {
    this.servicioService.listar().subscribe((servicios) => this.servicios.set(servicios));
    this.puestoService.listar().subscribe((puestos) => this.puestos.set(puestos));
    this.socioService.listar().subscribe((socios) => this.socios.set(socios));
  }

  seleccionarServicio(idTexto: string): void {
    this.servicioId.set(idTexto ? Number(idTexto) : null);
    this.puestoIdsSeleccionados.set(new Set());
    this.lecturasPorPuesto.set(new Map());
    this.etapasSeleccionadas.set(new Set());
    this.sociosUnicos.set(false);
    this.monto.set(null);
    this.cuentasGeneradas.set(null);
  }

  togglePuesto(id: number, incluido: boolean): void {
    this.puestoIdsSeleccionados.update((set) => {
      const nuevo = new Set(set);
      if (incluido) {
        nuevo.add(id);
      } else {
        nuevo.delete(id);
      }
      return nuevo;
    });
  }

  actualizarLectura(puesto: Puesto, campo: 'lecturaInicial' | 'lecturaFinal', valor: string): void {
    const numero = valor === '' ? null : Number(valor);
    this.lecturasPorPuesto.update((mapa) => {
      const nuevo = new Map(mapa);
      const fila = nuevo.get(puesto.id) ?? { puesto, lecturaInicial: null, lecturaFinal: null };
      nuevo.set(puesto.id, { ...fila, [campo]: numero });
      return nuevo;
    });
  }

  toggleEtapa(etapa: string, incluida: boolean): void {
    this.etapasSeleccionadas.update((set) => {
      const nuevo = new Set(set);
      if (incluida) {
        nuevo.add(etapa);
      } else {
        nuevo.delete(etapa);
      }
      return nuevo;
    });
  }

  generar(): void {
    const modo = this.modo();
    const servicio = this.servicioSeleccionado();
    if (!modo || !servicio) {
      return;
    }

    if (!this.periodo().trim()) {
      this.notificationService.error('Indica el período.');
      return;
    }

    if (modo === 'PUESTO_MONTO_FIJO') {
      this.generarPuestosMontoFijo(servicio.id);
    } else if (modo === 'PUESTO_CONSUMO') {
      this.generarPuestosConsumo(servicio.id);
    } else {
      this.generarSocios(servicio.id);
    }
  }

  private generarPuestosMontoFijo(servicioId: number): void {
    const puestoIds = Array.from(this.puestoIdsSeleccionados());
    if (puestoIds.length === 0) {
      this.notificationService.error('Selecciona al menos un puesto.');
      return;
    }
    if (!this.monto() || this.monto()! <= 0) {
      this.notificationService.error('Indica un monto mayor a 0.');
      return;
    }

    this.generando.set(true);
    this.cuentaService
      .generarParaPuestosMontoFijo({
        servicioId,
        periodo: this.periodo(),
        monto: this.monto()!,
        puestoIds,
      })
      .subscribe({
        next: (cuentas) => this.onGeneracionExitosa(cuentas),
        error: () => this.generando.set(false),
      });
  }

  private generarPuestosConsumo(servicioId: number): void {
    const lecturas: LecturaPuestoRequest[] = this.filasConsumo()
      .filter((fila) => fila.lecturaInicial !== null && fila.lecturaFinal !== null)
      .map((fila) => ({
        puestoId: fila.puesto.id,
        lecturaInicial: fila.lecturaInicial as number,
        lecturaFinal: fila.lecturaFinal as number,
      }));

    if (lecturas.length === 0) {
      this.notificationService.error('Registra la lectura inicial y final de al menos un puesto.');
      return;
    }

    this.generando.set(true);
    this.cuentaService
      .generarParaPuestosConsumo({ servicioId, periodo: this.periodo(), lecturas })
      .subscribe({
        next: (cuentas) => this.onGeneracionExitosa(cuentas),
        error: () => this.generando.set(false),
      });
  }

  private generarSocios(servicioId: number): void {
    if (!this.monto() || this.monto()! <= 0) {
      this.notificationService.error('Indica un monto mayor a 0.');
      return;
    }

    this.generando.set(true);
    this.cuentaService
      .generarParaSocios({
        servicioId,
        periodo: this.periodo(),
        monto: this.monto()!,
        etapas: Array.from(this.etapasSeleccionadas()),
        sociosUnicos: this.sociosUnicos(),
      })
      .subscribe({
        next: (cuentas) => this.onGeneracionExitosa(cuentas),
        error: () => this.generando.set(false),
      });
  }

  private onGeneracionExitosa(cuentas: CuentaPorCobrar[]): void {
    this.generando.set(false);
    this.cuentasGeneradas.set(cuentas);
    this.notificationService.exito(`Se generaron ${cuentas.length} cuenta(s) por cobrar.`);
  }
}
