package com.cibertec.sigac.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.sigac.dto.BancoResponse;
import com.cibertec.sigac.dto.CanjeBancarioRequest;
import com.cibertec.sigac.dto.CuentaPorCobrarResponse;
import com.cibertec.sigac.dto.GiroResponse;
import com.cibertec.sigac.dto.IngresoExternoRequest;
import com.cibertec.sigac.dto.ProcesarPagoRequest;
import com.cibertec.sigac.dto.ProcesarPagoResponse;
import com.cibertec.sigac.dto.PuestoResponse;
import com.cibertec.sigac.dto.ReciboResponse;
import com.cibertec.sigac.dto.ServicioCobrableResponse;
import com.cibertec.sigac.dto.SocioResponse;
import com.cibertec.sigac.entity.Banco;
import com.cibertec.sigac.entity.CuentaPorCobrar;
import com.cibertec.sigac.entity.EstadoCuenta;
import com.cibertec.sigac.entity.Puesto;
import com.cibertec.sigac.entity.Recibo;
import com.cibertec.sigac.entity.ServicioCobrable;
import com.cibertec.sigac.entity.Socio;
import com.cibertec.sigac.entity.TipoRecibo;
import com.cibertec.sigac.exception.BusinessRuleException;
import com.cibertec.sigac.exception.ResourceNotFoundException;
import com.cibertec.sigac.repository.BancoRepository;
import com.cibertec.sigac.repository.CuentaPorCobrarRepository;
import com.cibertec.sigac.repository.ReciboRepository;

import lombok.RequiredArgsConstructor;

/**
 * Correlativo de recibos: se apoya en el AUTO_INCREMENT (IDENTITY) de la
 * tabla "recibos". En MySQL no existen las secuencias nativas de
 * PostgreSQL/Oracle, pero el contador AUTO_INCREMENT de InnoDB cumple el
 * mismo rol y es seguro ante concurrencia (el motor asigna el siguiente
 * valor bajo un lock interno muy breve, sin que dos inserciones puedan
 * recibir el mismo id). Por eso el correlativo se deriva del id generado
 * en vez de mantener un contador propio con bloqueo optimista: un contador
 * manual (columna con @Version) solo detecta colisiones y obliga a
 * reintentar, mientras que AUTO_INCREMENT las evita de raiz sin retries.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PagoServiceImpl implements PagoService {

    private static final String PREFIJO_CORRELATIVO = "R-";

    private final CuentaPorCobrarRepository cuentaPorCobrarRepository;
    private final ReciboRepository reciboRepository;
    private final BancoRepository bancoRepository;

    @Override
    public ProcesarPagoResponse procesarPagoCuentas(ProcesarPagoRequest request) {
        if (request.getCuentasAbonadasIds().isEmpty() && request.getCuentasExoneradasIds().isEmpty()) {
            throw new BusinessRuleException("Debes seleccionar al menos una cuenta para procesar");
        }

        List<Long> idsRepetidos = request.getCuentasAbonadasIds().stream()
                .filter(request.getCuentasExoneradasIds()::contains)
                .toList();
        if (!idsRepetidos.isEmpty()) {
            throw new BusinessRuleException("Una cuenta no puede marcarse como abonada y exonerada a la vez");
        }

        List<CuentaPorCobrar> exoneradas = obtenerPendientesOLanzar(request.getCuentasExoneradasIds());
        exoneradas.forEach(cuenta -> cuenta.setEstado(EstadoCuenta.EXONERADA));
        cuentaPorCobrarRepository.saveAll(exoneradas);

        List<CuentaPorCobrar> abonadas = obtenerPendientesOLanzar(request.getCuentasAbonadasIds());

        Recibo recibo = null;
        if (!abonadas.isEmpty()) {
            BigDecimal total = abonadas.stream().map(CuentaPorCobrar::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

            recibo = crearReciboConCorrelativo(Recibo.builder()
                    .tipo(TipoRecibo.PAGO_CUENTAS)
                    .fecha(LocalDateTime.now())
                    .monto(total)
                    .build());

            Recibo reciboFinal = recibo;
            abonadas.forEach(cuenta -> {
                cuenta.setEstado(EstadoCuenta.ABONADA);
                cuenta.setRecibo(reciboFinal);
            });
            cuentaPorCobrarRepository.saveAll(abonadas);
        }

        List<CuentaPorCobrar> actualizadas = new ArrayList<>(exoneradas);
        actualizadas.addAll(abonadas);

        return ProcesarPagoResponse.builder()
                .recibo(recibo != null ? toReciboResponse(recibo, abonadas) : null)
                .cuentasActualizadas(actualizadas.stream().map(this::toCuentaResponse).toList())
                .build();
    }

    @Override
    public ReciboResponse canjeBancario(CanjeBancarioRequest request) {
        CuentaPorCobrar cuenta = obtenerPendienteOLanzar(request.getCuentaId());

        if (cuenta.getSocio() == null) {
            throw new BusinessRuleException("El canje bancario solo aplica a cuentas de socio");
        }

        Banco banco = bancoRepository.findById(request.getBancoId())
                .orElseThrow(() -> new ResourceNotFoundException("Banco no encontrado con id " + request.getBancoId()));

        Recibo recibo = crearReciboConCorrelativo(Recibo.builder()
                .tipo(TipoRecibo.CANJE_BANCARIO)
                .fecha(LocalDateTime.now())
                .monto(cuenta.getMonto())
                .banco(banco)
                .fechaDeposito(request.getFechaDeposito())
                .build());

        cuenta.setEstado(EstadoCuenta.ABONADA);
        cuenta.setRecibo(recibo);
        cuentaPorCobrarRepository.save(cuenta);

        return toReciboResponse(recibo, List.of(cuenta));
    }

    @Override
    public ReciboResponse registrarIngresoExterno(IngresoExternoRequest request) {
        Recibo recibo = crearReciboConCorrelativo(Recibo.builder()
                .tipo(TipoRecibo.INGRESO_EXTERNO)
                .fecha(LocalDateTime.now())
                .monto(request.getMonto())
                .depositante(request.getDepositante())
                .categoria(request.getCategoria())
                .concepto(request.getConcepto())
                .build());

        return toReciboResponse(recibo, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReciboResponse> listarRecibos() {
        return reciboRepository.findAllByOrderByIdDesc().stream()
                .map(recibo -> toReciboResponse(recibo, cuentaPorCobrarRepository.findByReciboId(recibo.getId())))
                .toList();
    }

    private Recibo crearReciboConCorrelativo(Recibo recibo) {
        Recibo guardado = reciboRepository.save(recibo);
        guardado.setCorrelativo(PREFIJO_CORRELATIVO + String.format("%06d", guardado.getId()));
        return reciboRepository.save(guardado);
    }

    private List<CuentaPorCobrar> obtenerPendientesOLanzar(List<Long> ids) {
        return ids.stream().map(this::obtenerPendienteOLanzar).toList();
    }

    private CuentaPorCobrar obtenerPendienteOLanzar(Long id) {
        CuentaPorCobrar cuenta = cuentaPorCobrarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por cobrar no encontrada con id " + id));

        if (cuenta.getEstado() != EstadoCuenta.PENDIENTE) {
            throw new BusinessRuleException(
                    "La cuenta " + id + " ya fue procesada (estado actual: " + cuenta.getEstado() + ")");
        }

        return cuenta;
    }

    private ReciboResponse toReciboResponse(Recibo recibo, List<CuentaPorCobrar> cuentas) {
        return ReciboResponse.builder()
                .id(recibo.getId())
                .correlativo(recibo.getCorrelativo())
                .tipo(recibo.getTipo())
                .fecha(recibo.getFecha())
                .monto(recibo.getMonto())
                .banco(toBancoResponse(recibo.getBanco()))
                .fechaDeposito(recibo.getFechaDeposito())
                .depositante(recibo.getDepositante())
                .categoria(recibo.getCategoria())
                .concepto(recibo.getConcepto())
                .cuentas(cuentas.stream().map(this::toCuentaResponse).toList())
                .build();
    }

    private CuentaPorCobrarResponse toCuentaResponse(CuentaPorCobrar cuenta) {
        return CuentaPorCobrarResponse.builder()
                .id(cuenta.getId())
                .estado(cuenta.getEstado())
                .monto(cuenta.getMonto())
                .periodo(cuenta.getPeriodo())
                .puesto(toPuestoResponse(cuenta.getPuesto()))
                .socio(toSocioResponse(cuenta.getSocio()))
                .servicio(toServicioResponse(cuenta.getServicio()))
                .lecturaInicial(cuenta.getLecturaInicial())
                .lecturaFinal(cuenta.getLecturaFinal())
                .reciboCorrelativo(cuenta.getRecibo() != null ? cuenta.getRecibo().getCorrelativo() : null)
                .build();
    }

    private PuestoResponse toPuestoResponse(Puesto puesto) {
        if (puesto == null) {
            return null;
        }
        return PuestoResponse.builder()
                .id(puesto.getId())
                .numero(puesto.getNumero())
                .nombreInquilino(puesto.getNombreInquilino())
                .fechaInicioVigencia(puesto.getFechaInicioVigencia())
                .fechaFinVigencia(puesto.getFechaFinVigencia())
                .giro(GiroResponse.builder().id(puesto.getGiro().getId()).nombre(puesto.getGiro().getNombre()).build())
                .socio(toSocioResponse(puesto.getSocio()))
                .build();
    }

    private SocioResponse toSocioResponse(Socio socio) {
        if (socio == null) {
            return null;
        }
        return SocioResponse.builder()
                .id(socio.getId())
                .codigo(socio.getCodigo())
                .nombres(socio.getNombres())
                .apellidos(socio.getApellidos())
                .accion(socio.getAccion())
                .etapa(socio.getEtapa())
                .fechaNacimiento(socio.getFechaNacimiento())
                .build();
    }

    private ServicioCobrableResponse toServicioResponse(ServicioCobrable servicio) {
        return ServicioCobrableResponse.builder()
                .id(servicio.getId())
                .nombre(servicio.getNombre())
                .recurrencia(servicio.getRecurrencia())
                .costo(servicio.getCosto())
                .moneda(servicio.getMoneda())
                .destinatario(servicio.getDestinatario())
                .esPorConsumo(servicio.isEsPorConsumo())
                .build();
    }

    private BancoResponse toBancoResponse(Banco banco) {
        if (banco == null) {
            return null;
        }
        return BancoResponse.builder()
                .id(banco.getId())
                .nombre(banco.getNombre())
                .numeroCuenta(banco.getNumeroCuenta())
                .cci(banco.getCci())
                .moneda(banco.getMoneda())
                .build();
    }
}
