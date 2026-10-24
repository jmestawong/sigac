package com.cibertec.mscobranza.negocio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.mscobranza.client.CatalogosClient;
import com.cibertec.mscobranza.dto.BancoResponse;
import com.cibertec.mscobranza.dto.CanjeBancarioRequest;
import com.cibertec.mscobranza.dto.IngresoExternoRequest;
import com.cibertec.mscobranza.dto.ProcesarPagoRequest;
import com.cibertec.mscobranza.dto.ProcesarPagoResponse;
import com.cibertec.mscobranza.dto.ReciboResponse;
import com.cibertec.mscobranza.entidades.CuentaPorCobrar;
import com.cibertec.mscobranza.entidades.EstadoCuenta;
import com.cibertec.mscobranza.entidades.Recibo;
import com.cibertec.mscobranza.entidades.TipoRecibo;
import com.cibertec.mscobranza.repositorio.CuentaPorCobrarRepository;
import com.cibertec.mscobranza.repositorio.ReciboRepository;
import com.cibertec.sigac.common.auditoria.AuditoriaPublisher;
import com.cibertec.sigac.common.exception.BusinessRuleException;
import com.cibertec.sigac.common.exception.ResourceNotFoundException;

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
 *
 * Los pagos son transaccionales: las cuentas y el recibo viven en la misma
 * base de datos de este servicio, asi que se confirman o se revierten juntos.
 * La auditoria se publica en RabbitMQ de forma asincrona y no afecta al cobro.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PagoServiceImpl implements PagoService {

    private static final String PREFIJO_CORRELATIVO = "R-";

    private final CuentaPorCobrarRepository cuentaPorCobrarRepository;
    private final ReciboRepository reciboRepository;
    private final CatalogosClient catalogosClient;
    private final CobranzaMapper mapper;
    private final AuditoriaPublisher auditoria;

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

            auditoria.registrar("COBRANZA", "RECIBO_EMITIDO", recibo.getCorrelativo(),
                    abonadas.size() + " cuenta(s) abonada(s), " + exoneradas.size() + " exonerada(s)", total);
        } else {
            auditoria.registrar("COBRANZA", "CUENTAS_EXONERADAS", exoneradas.size() + " cuenta(s)",
                    "Exoneracion sin recibo", BigDecimal.ZERO);
        }

        List<CuentaPorCobrar> actualizadas = new ArrayList<>(exoneradas);
        actualizadas.addAll(abonadas);

        CatalogoResolver catalogos = mapper.nuevoResolver();
        return ProcesarPagoResponse.builder()
                .recibo(recibo != null ? mapper.toReciboResponse(recibo, abonadas, catalogos) : null)
                .cuentasActualizadas(mapper.toCuentaResponses(actualizadas, catalogos))
                .build();
    }

    @Override
    public ReciboResponse canjeBancario(CanjeBancarioRequest request) {
        CuentaPorCobrar cuenta = obtenerPendienteOLanzar(request.getCuentaId());

        if (cuenta.getSocioId() == null) {
            throw new BusinessRuleException("El canje bancario solo aplica a cuentas de socio");
        }

        BancoResponse banco = catalogosClient.obtenerBanco(request.getBancoId());

        Recibo recibo = crearReciboConCorrelativo(Recibo.builder()
                .tipo(TipoRecibo.CANJE_BANCARIO)
                .fecha(LocalDateTime.now())
                .monto(cuenta.getMonto())
                .bancoId(banco.getId())
                .fechaDeposito(request.getFechaDeposito())
                .build());

        cuenta.setEstado(EstadoCuenta.ABONADA);
        cuenta.setRecibo(recibo);
        cuentaPorCobrarRepository.save(cuenta);

        auditoria.registrar("COBRANZA", "CANJE_BANCARIO", recibo.getCorrelativo(),
                "Banco " + banco.getNombre() + ", deposito " + request.getFechaDeposito(), recibo.getMonto());

        return mapper.toReciboResponse(recibo, List.of(cuenta), mapper.nuevoResolver());
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

        auditoria.registrar("COBRANZA", "INGRESO_EXTERNO", recibo.getCorrelativo(),
                request.getDepositante() + " - " + request.getConcepto(), recibo.getMonto());

        return mapper.toReciboResponse(recibo, List.of(), mapper.nuevoResolver());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReciboResponse> listarRecibos(LocalDate fecha, TipoRecibo tipo) {
        CatalogoResolver catalogos = mapper.nuevoResolver();
        return reciboRepository.findAllByOrderByIdDesc().stream()
                .filter(recibo -> fecha == null || recibo.getFecha().toLocalDate().isEqual(fecha))
                .filter(recibo -> tipo == null || recibo.getTipo() == tipo)
                .map(recibo -> mapper.toReciboResponse(recibo, cuentaPorCobrarRepository.findByReciboId(recibo.getId()),
                        catalogos))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReciboResponse obtenerReciboPorId(Long id) {
        Recibo recibo = reciboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recibo no encontrado con id " + id));
        return mapper.toReciboResponse(recibo, cuentaPorCobrarRepository.findByReciboId(id), mapper.nuevoResolver());
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
}
