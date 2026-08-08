package com.cibertec.sigac.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.sigac.dto.BancoResponse;
import com.cibertec.sigac.dto.CuentaPorCobrarResponse;
import com.cibertec.sigac.dto.GenerarPuestosConsumoRequest;
import com.cibertec.sigac.dto.GenerarPuestosMontoFijoRequest;
import com.cibertec.sigac.dto.GenerarSociosRequest;
import com.cibertec.sigac.dto.GiroResponse;
import com.cibertec.sigac.dto.LecturaPuestoRequest;
import com.cibertec.sigac.dto.PuestoResponse;
import com.cibertec.sigac.dto.ReciboResponse;
import com.cibertec.sigac.dto.ResumenPuestoResponse;
import com.cibertec.sigac.dto.ResumenSocioResponse;
import com.cibertec.sigac.dto.ServicioCobrableResponse;
import com.cibertec.sigac.dto.SocioResponse;
import com.cibertec.sigac.entity.Banco;
import com.cibertec.sigac.entity.CuentaPorCobrar;
import com.cibertec.sigac.entity.EstadoCuenta;
import com.cibertec.sigac.entity.Puesto;
import com.cibertec.sigac.entity.Recibo;
import com.cibertec.sigac.entity.ServicioCobrable;
import com.cibertec.sigac.entity.Socio;
import com.cibertec.sigac.entity.TipoDestinatario;
import com.cibertec.sigac.exception.BusinessRuleException;
import com.cibertec.sigac.exception.ResourceNotFoundException;
import com.cibertec.sigac.repository.CuentaPorCobrarRepository;
import com.cibertec.sigac.repository.PuestoRepository;
import com.cibertec.sigac.repository.ServicioCobrableRepository;
import com.cibertec.sigac.repository.SocioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CuentaPorCobrarServiceImpl implements CuentaPorCobrarService {

    private final CuentaPorCobrarRepository cuentaPorCobrarRepository;
    private final ServicioCobrableRepository servicioCobrableRepository;
    private final PuestoRepository puestoRepository;
    private final SocioRepository socioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorCobrarResponse> listarTodas() {
        return cuentaPorCobrarRepository.findAllByOrderByIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaPorCobrarResponse obtenerPorId(Long id) {
        return toResponse(buscarOLanzar(id));
    }

    @Override
    public void eliminar(Long id) {
        cuentaPorCobrarRepository.delete(buscarOLanzar(id));
    }

    @Override
    public List<CuentaPorCobrarResponse> generarParaPuestosMontoFijo(GenerarPuestosMontoFijoRequest request) {
        ServicioCobrable servicio = buscarServicio(request.getServicioId());
        validarDestinatario(servicio, TipoDestinatario.PUESTO);

        if (servicio.isEsPorConsumo()) {
            throw new BusinessRuleException(
                    "El servicio '" + servicio.getNombre() + "' es por consumo; usa la generacion por lecturas.");
        }

        List<CuentaPorCobrar> cuentas = request.getPuestoIds().stream()
                .distinct()
                .map(this::buscarPuesto)
                .map(puesto -> CuentaPorCobrar.builder()
                        .estado(EstadoCuenta.PENDIENTE)
                        .monto(request.getMonto())
                        .periodo(request.getPeriodo())
                        .puesto(puesto)
                        .servicio(servicio)
                        .build())
                .toList();

        return guardarYMapear(cuentas);
    }

    @Override
    public List<CuentaPorCobrarResponse> generarParaPuestosConsumo(GenerarPuestosConsumoRequest request) {
        ServicioCobrable servicio = buscarServicio(request.getServicioId());
        validarDestinatario(servicio, TipoDestinatario.PUESTO);

        if (!servicio.isEsPorConsumo()) {
            throw new BusinessRuleException(
                    "El servicio '" + servicio.getNombre() + "' no es por consumo; usa la generacion por monto fijo.");
        }

        List<CuentaPorCobrar> cuentas = request.getLecturas().stream()
                .map(lectura -> crearCuentaPorConsumo(servicio, request.getPeriodo(), lectura))
                .toList();

        return guardarYMapear(cuentas);
    }

    @Override
    public List<CuentaPorCobrarResponse> generarParaSocios(GenerarSociosRequest request) {
        ServicioCobrable servicio = buscarServicio(request.getServicioId());
        validarDestinatario(servicio, TipoDestinatario.SOCIO);

        List<Socio> candidatos = socioRepository.findAll();

        if (request.getEtapas() != null && !request.getEtapas().isEmpty()) {
            candidatos = candidatos.stream()
                    .filter(socio -> request.getEtapas().contains(socio.getEtapa()))
                    .toList();
        }

        if (request.isSociosUnicos()) {
            candidatos = deduplicarPorNombreYApellido(candidatos);
        }

        List<CuentaPorCobrar> cuentas = candidatos.stream()
                .map(socio -> CuentaPorCobrar.builder()
                        .estado(EstadoCuenta.PENDIENTE)
                        .monto(request.getMonto())
                        .periodo(request.getPeriodo())
                        .socio(socio)
                        .servicio(servicio)
                        .build())
                .toList();

        return guardarYMapear(cuentas);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenSocioResponse resumenPorSocio(Long socioId) {
        Socio socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new ResourceNotFoundException("Socio no encontrado con id " + socioId));

        List<CuentaPorCobrar> cuentasSocio = cuentaPorCobrarRepository.findBySocioIdOrderByIdDesc(socioId);
        List<CuentaPorCobrar> cuentasPuestos = cuentaPorCobrarRepository.findByPuesto_Socio_IdOrderByIdDesc(socioId);

        return ResumenSocioResponse.builder()
                .socio(toSocioResponse(socio))
                .cuentasSocio(cuentasSocio.stream().map(this::toResponse).toList())
                .cuentasPuestos(cuentasPuestos.stream().map(this::toResponse).toList())
                .movimientos(extraerRecibosUnicos(cuentasSocio, cuentasPuestos).stream()
                        .map(this::toReciboResponse)
                        .toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenPuestoResponse resumenPorPuesto(Long puestoId) {
        Puesto puesto = buscarPuesto(puestoId);

        List<CuentaPorCobrar> cuentasPuesto = cuentaPorCobrarRepository.findByPuestoIdOrderByIdDesc(puestoId);
        List<CuentaPorCobrar> cuentasSocioAsociado = puesto.getSocio() != null
                ? cuentaPorCobrarRepository.findBySocioIdOrderByIdDesc(puesto.getSocio().getId())
                : List.of();

        return ResumenPuestoResponse.builder()
                .puesto(toPuestoResponse(puesto))
                .cuentasPuesto(cuentasPuesto.stream().map(this::toResponse).toList())
                .cuentasSocioAsociado(cuentasSocioAsociado.stream().map(this::toResponse).toList())
                .movimientos(extraerRecibosUnicos(cuentasPuesto, cuentasSocioAsociado).stream()
                        .map(this::toReciboResponse)
                        .toList())
                .build();
    }

    @SafeVarargs
    private List<Recibo> extraerRecibosUnicos(List<CuentaPorCobrar>... listas) {
        Map<Long, Recibo> unicos = new LinkedHashMap<>();
        for (List<CuentaPorCobrar> lista : listas) {
            for (CuentaPorCobrar cuenta : lista) {
                if (cuenta.getRecibo() != null) {
                    unicos.putIfAbsent(cuenta.getRecibo().getId(), cuenta.getRecibo());
                }
            }
        }
        return new ArrayList<>(unicos.values());
    }

    private ReciboResponse toReciboResponse(Recibo recibo) {
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
                .cuentas(List.of())
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

    private CuentaPorCobrar crearCuentaPorConsumo(ServicioCobrable servicio, String periodo, LecturaPuestoRequest lectura) {
        Puesto puesto = buscarPuesto(lectura.getPuestoId());
        BigDecimal diferencia = lectura.getLecturaFinal().subtract(lectura.getLecturaInicial());
        BigDecimal importe = diferencia.compareTo(BigDecimal.ZERO) > 0
                ? diferencia.multiply(servicio.getCosto())
                : BigDecimal.ZERO;

        return CuentaPorCobrar.builder()
                .estado(EstadoCuenta.PENDIENTE)
                .monto(importe)
                .periodo(periodo)
                .puesto(puesto)
                .servicio(servicio)
                .lecturaInicial(lectura.getLecturaInicial())
                .lecturaFinal(lectura.getLecturaFinal())
                .build();
    }

    private List<Socio> deduplicarPorNombreYApellido(List<Socio> socios) {
        Map<String, Socio> unicos = new LinkedHashMap<>();
        for (Socio socio : socios) {
            String clave = (socio.getNombres().trim() + "|" + socio.getApellidos().trim()).toLowerCase();
            unicos.putIfAbsent(clave, socio);
        }
        return new ArrayList<>(unicos.values());
    }

    private void validarDestinatario(ServicioCobrable servicio, TipoDestinatario esperado) {
        if (servicio.getDestinatario() != esperado) {
            throw new BusinessRuleException(
                    "El servicio '" + servicio.getNombre() + "' no esta configurado para destinatario " + esperado);
        }
    }

    private List<CuentaPorCobrarResponse> guardarYMapear(List<CuentaPorCobrar> cuentas) {
        return cuentaPorCobrarRepository.saveAll(cuentas).stream()
                .map(this::toResponse)
                .toList();
    }

    private ServicioCobrable buscarServicio(Long id) {
        return servicioCobrableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio cobrable no encontrado con id " + id));
    }

    private Puesto buscarPuesto(Long id) {
        return puestoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Puesto no encontrado con id " + id));
    }

    private CuentaPorCobrar buscarOLanzar(Long id) {
        return cuentaPorCobrarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por cobrar no encontrada con id " + id));
    }

    private CuentaPorCobrarResponse toResponse(CuentaPorCobrar cuenta) {
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
}
