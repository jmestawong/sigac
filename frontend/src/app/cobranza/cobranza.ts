import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';

import { CuentaPorCobrarService } from '../core/services/cuenta-por-cobrar.service';
import { BancoService } from '../core/services/banco.service';
import { NotificationService } from '../core/services/notification.service';
import { PagoService } from '../core/services/pago.service';
import { PuestoService } from '../core/services/puesto.service';
import { SocioService } from '../core/services/socio.service';
import { Banco } from '../core/models/banco.model';
import { CuentaPorCobrar, ResumenPuesto, ResumenSocio } from '../core/models/cuenta-por-cobrar.model';
import { Puesto } from '../core/models/puesto.model';
import { Recibo } from '../core/models/recibo.model';
import { Socio } from '../core/models/socio.model';

type Modo = 'SOCIO' | 'PUESTO';
type Decision = 'ABONAR' | 'EXONERAR';

@Component({
  selector: 'app-cobranza',
  imports: [DatePipe],
  templateUrl: './cobranza.html',
  styleUrl: './cobranza.css',
})
export class Cobranza {
  protected readonly Number = Number;
  private readonly cuentaService = inject(CuentaPorCobrarService);
  private readonly pagoService = inject(PagoService);
  private readonly socioService = inject(SocioService);
  private readonly puestoService = inject(PuestoService);
  private readonly bancoService = inject(BancoService);
  private readonly notificationService = inject(NotificationService);

  readonly modo = signal<Modo>('SOCIO');
  readonly filtroTexto = signal('');

  readonly socios = signal<Socio[]>([]);
  readonly puestos = signal<Puesto[]>([]);
  readonly bancos = signal<Banco[]>([]);

  readonly sociosFiltrados = computed(() => {
    const termino = this.filtroTexto().trim().toLowerCase();
    if (!termino) {
      return this.socios();
    }
    return this.socios().filter((s) => `${s.codigo} ${s.nombres} ${s.apellidos}`.toLowerCase().includes(termino));
  });

  readonly puestosFiltrados = computed(() => {
    const termino = this.filtroTexto().trim().toLowerCase();
    if (!termino) {
      return this.puestos();
    }
    return this.puestos().filter((p) => `${p.numero} ${p.nombreInquilino}`.toLowerCase().includes(termino));
  });

  readonly seleccionadoId = signal<number | null>(null);
  readonly resumenSocio = signal<ResumenSocio | null>(null);
  readonly resumenPuesto = signal<ResumenPuesto | null>(null);
  readonly cargandoResumen = signal(false);

  readonly decisiones = signal<Map<number, Decision>>(new Map());
  readonly procesando = signal(false);
  readonly ultimoRecibo = signal<Recibo | null>(null);

  readonly canjeAbiertoParaCuenta = signal<number | null>(null);
  readonly canjeBancoId = signal<number | null>(null);
  readonly canjeFecha = signal('');

  readonly totalAPagar = computed(() => {
    const cuentas = this.todasLasCuentas();
    let total = 0;
    for (const [cuentaId, decision] of this.decisiones()) {
      if (decision === 'ABONAR') {
        const cuenta = cuentas.find((c) => c.id === cuentaId);
        if (cuenta) {
          total += cuenta.monto;
        }
      }
    }
    return total;
  });

  constructor() {
    this.socioService.listar().subscribe((socios) => this.socios.set(socios));
    this.puestoService.listar().subscribe((puestos) => this.puestos.set(puestos));
    this.bancoService.listar().subscribe((bancos) => this.bancos.set(bancos));
  }

  private todasLasCuentas(): CuentaPorCobrar[] {
    const rs = this.resumenSocio();
    if (rs) {
      return [...rs.cuentasSocio, ...rs.cuentasPuestos];
    }
    const rp = this.resumenPuesto();
    if (rp) {
      return [...rp.cuentasPuesto, ...rp.cuentasSocioAsociado];
    }
    return [];
  }

  cambiarModo(modo: Modo): void {
    this.modo.set(modo);
    this.filtroTexto.set('');
    this.limpiarSeleccion();
  }

  private limpiarSeleccion(): void {
    this.seleccionadoId.set(null);
    this.resumenSocio.set(null);
    this.resumenPuesto.set(null);
    this.decisiones.set(new Map());
    this.ultimoRecibo.set(null);
    this.canjeAbiertoParaCuenta.set(null);
  }

  seleccionar(id: number): void {
    this.seleccionadoId.set(id);
    this.decisiones.set(new Map());
    this.ultimoRecibo.set(null);
    this.cargarResumen();
  }

  private cargarResumen(): void {
    const id = this.seleccionadoId();
    if (id === null) {
      return;
    }

    this.cargandoResumen.set(true);

    if (this.modo() === 'SOCIO') {
      this.cuentaService.resumenPorSocio(id).subscribe({
        next: (resumen) => {
          this.resumenSocio.set(resumen);
          this.cargandoResumen.set(false);
        },
        error: () => this.cargandoResumen.set(false),
      });
    } else {
      this.cuentaService.resumenPorPuesto(id).subscribe({
        next: (resumen) => {
          this.resumenPuesto.set(resumen);
          this.cargandoResumen.set(false);
        },
        error: () => this.cargandoResumen.set(false),
      });
    }
  }

  toggleCuenta(cuentaId: number, marcada: boolean): void {
    this.decisiones.update((mapa) => {
      const nuevo = new Map(mapa);
      if (marcada) {
        nuevo.set(cuentaId, 'ABONAR');
      } else {
        nuevo.delete(cuentaId);
      }
      return nuevo;
    });
  }

  cambiarDecision(cuentaId: number, decision: Decision): void {
    this.decisiones.update((mapa) => {
      const nuevo = new Map(mapa);
      nuevo.set(cuentaId, decision);
      return nuevo;
    });
  }

  decisionDe(cuentaId: number): Decision | undefined {
    return this.decisiones().get(cuentaId);
  }

  confirmarPago(): void {
    const decisiones = this.decisiones();
    if (decisiones.size === 0) {
      this.notificationService.error('Selecciona al menos una cuenta para procesar.');
      return;
    }

    const cuentasAbonadasIds: number[] = [];
    const cuentasExoneradasIds: number[] = [];
    for (const [cuentaId, decision] of decisiones) {
      (decision === 'ABONAR' ? cuentasAbonadasIds : cuentasExoneradasIds).push(cuentaId);
    }

    this.procesando.set(true);
    this.pagoService.procesarPagoCuentas({ cuentasAbonadasIds, cuentasExoneradasIds }).subscribe({
      next: (respuesta) => {
        this.procesando.set(false);
        this.ultimoRecibo.set(respuesta.recibo);
        this.decisiones.set(new Map());
        this.notificationService.exito(
          respuesta.recibo
            ? `Pago procesado. Recibo ${respuesta.recibo.correlativo} por ${respuesta.recibo.monto}.`
            : 'Cuentas exoneradas correctamente.',
        );
        this.cargarResumen();
      },
      error: () => this.procesando.set(false),
    });
  }

  abrirCanje(cuentaId: number): void {
    this.canjeAbiertoParaCuenta.set(cuentaId);
    this.canjeBancoId.set(null);
    this.canjeFecha.set('');
  }

  cerrarCanje(): void {
    this.canjeAbiertoParaCuenta.set(null);
  }

  confirmarCanje(cuentaId: number): void {
    const bancoId = this.canjeBancoId();
    const fecha = this.canjeFecha();

    if (!bancoId || !fecha) {
      this.notificationService.error('Selecciona el banco y la fecha de depósito.');
      return;
    }

    this.pagoService.canjeBancario({ cuentaId, bancoId, fechaDeposito: fecha }).subscribe({
      next: (recibo) => {
        this.ultimoRecibo.set(recibo);
        this.canjeAbiertoParaCuenta.set(null);
        this.notificationService.exito(`Canje registrado. Recibo ${recibo.correlativo}.`);
        this.cargarResumen();
      },
    });
  }
}
