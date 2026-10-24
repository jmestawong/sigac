package com.cibertec.mscobranza.negocio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.mscobranza.client.CatalogosClient;
import com.cibertec.mscobranza.dto.CuentaPorCobrarResponse;
import com.cibertec.mscobranza.dto.GenerarPuestosConsumoRequest;
import com.cibertec.mscobranza.dto.GenerarPuestosMontoFijoRequest;
import com.cibertec.mscobranza.dto.GenerarSociosRequest;
import com.cibertec.mscobranza.dto.LecturaPuestoRequest;
import com.cibertec.mscobranza.dto.PuestoResponse;
import com.cibertec.mscobranza.dto.ReciboResponse;
import com.cibertec.mscobranza.dto.ReferenciaResponse;
import com.cibertec.mscobranza.dto.ResumenPuestoResponse;
import com.cibertec.mscobranza.dto.ResumenSocioResponse;
import com.cibertec.mscobranza.dto.ServicioCobrableResponse;
import com.cibertec.mscobranza.dto.SocioResponse;
import com.cibertec.mscobranza.dto.TipoDestinatario;
import com.cibertec.mscobranza.entidades.CuentaPorCobrar;
import com.cibertec.mscobranza.entidades.EstadoCuenta;
import com.cibertec.mscobranza.entidades.Recibo;
import com.cibertec.mscobranza.repositorio.CuentaPorCobrarRepository;
import com.cibertec.mscobranza.repositorio.ReciboRepository;
import com.cibertec.sigac.common.auditoria.AuditoriaPublisher;
import com.cibertec.sigac.common.exception.BusinessRuleException;
import com.cibertec.sigac.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CuentaPorCobrarServiceImpl implements CuentaPorCobrarService {

    private final CuentaPorCobrarRepository cuentaPorCobrarRepository;
    private final ReciboRepository reciboRepository;
    private final CatalogosClient catalogosClient;
    private final CobranzaMapper mapper;
    private final AuditoriaPublisher auditoria;

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorCobrarResponse> listarTodas() {
        return mapper.toCuentaResponses(cuentaPorCobrarRepository.findAllByOrderByIdDesc(), mapper.nuevoResolver());
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaPorCobrarResponse obtenerPorId(Long id) {
        return mapper.toCuentaResponse(buscarOLanzar(id), mapper.nuevoResolver());
    }

    @Override
    public void eliminar(Long id) {
        CuentaPorCobrar cuenta = buscarOLanzar(id);
        cuentaPorCobrarRepository.delete(cuenta);
        auditoria.registrar("COBRANZA", "CUENTA_ELIMINADA", "cuenta " + id,
                "Periodo " + cuenta.getPeriodo() + ", estado " + cuenta.getEstado(), cuenta.getMonto());
    }

    @Override
    public List<CuentaPorCobrarResponse> generarParaPuestosMontoFijo(GenerarPuestosMontoFijoRequest request) {
        ServicioCobrableResponse servicio = catalogosClient.obtenerServicio(request.getServicioId());
        validarDestinatario(servicio, TipoDestinatario.PUESTO);

        if (servicio.isEsPorConsumo()) {
            throw new BusinessRuleException(
                    "El servicio '" + servicio.getNombre() + "' es por consumo; usa la generacion por lecturas.");
        }

        List<CuentaPorCobrar> cuentas = request.getPuestoIds().stream()
                .distinct()
                .map(this::validarPuesto)
                .map(puestoId -> CuentaPorCobrar.builder()
                        .estado(EstadoCuenta.PENDIENTE)
                        .monto(request.getMonto())
                        .periodo(request.getPeriodo())
                        .puestoId(puestoId)
                        .servicioId(servicio.getId())
                        .build())
                .toList();

        return guardarYMapear(cuentas, servicio, request.getPeriodo());
    }

    @Override
    public List<CuentaPorCobrarResponse> generarParaPuestosConsumo(GenerarPuestosConsumoRequest request) {
        ServicioCobrableResponse servicio = catalogosClient.obtenerServicio(request.getServicioId());
        validarDestinatario(servicio, TipoDestinatario.PUESTO);

        if (!servicio.isEsPorConsumo()) {
            throw new BusinessRuleException(
                    "El servicio '" + servicio.getNombre() + "' no es por consumo; usa la generacion por monto fijo.");
        }

        List<CuentaPorCobrar> cuentas = request.getLecturas().stream()
                .map(lectura -> crearCuentaPorConsumo(servicio, request.getPeriodo(), lectura))
                .toList();

        return guardarYMapear(cuentas, servicio, request.getPeriodo());
    }

    @Override
    public List<CuentaPorCobrarResponse> generarParaSocios(GenerarSociosRequest request) {
        ServicioCobrableResponse servicio = catalogosClient.obtenerServicio(request.getServicioId());
        validarDestinatario(servicio, TipoDestinatario.SOCIO);

        List<SocioResponse> candidatos = catalogosClient.listarSocios();

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
                        .socioId(socio.getId())
                        .servicioId(servicio.getId())
                        .build())
                .toList();

        return guardarYMapear(cuentas, servicio, request.getPeriodo());
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenSocioResponse resumenPorSocio(Long socioId) {
        CatalogoResolver catalogos = mapper.nuevoResolver();
        SocioResponse socio = catalogosClient.obtenerSocio(socioId);

        List<Long> puestosDelSocio = catalogosClient.listarPuestos().stream()
                .filter(puesto -> puesto.getSocio() != null && socioId.equals(puesto.getSocio().getId()))
                .map(PuestoResponse::getId)
                .toList();

        List<CuentaPorCobrar> cuentasSocio = cuentaPorCobrarRepository.findBySocioIdOrderByIdDesc(socioId);
        List<CuentaPorCobrar> cuentasPuestos = puestosDelSocio.isEmpty()
                ? List.of()
                : cuentaPorCobrarRepository.findByPuestoIdInOrderByIdDesc(puestosDelSocio);

        return ResumenSocioResponse.builder()
                .socio(socio)
                .cuentasSocio(mapper.toCuentaResponses(cuentasSocio, catalogos))
                .cuentasPuestos(mapper.toCuentaResponses(cuentasPuestos, catalogos))
                .movimientos(extraerRecibosUnicos(cuentasSocio, cuentasPuestos).stream()
                        .map(recibo -> mapper.toReciboResponse(recibo, List.of(), catalogos))
                        .toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenPuestoResponse resumenPorPuesto(Long puestoId) {
        CatalogoResolver catalogos = mapper.nuevoResolver();
        PuestoResponse puesto = catalogosClient.obtenerPuesto(puestoId);

        List<CuentaPorCobrar> cuentasPuesto = cuentaPorCobrarRepository.findByPuestoIdOrderByIdDesc(puestoId);
        List<CuentaPorCobrar> cuentasSocioAsociado = puesto.getSocio() != null
                ? cuentaPorCobrarRepository.findBySocioIdOrderByIdDesc(puesto.getSocio().getId())
                : List.of();

        return ResumenPuestoResponse.builder()
                .puesto(puesto)
                .cuentasPuesto(mapper.toCuentaResponses(cuentasPuesto, catalogos))
                .cuentasSocioAsociado(mapper.toCuentaResponses(cuentasSocioAsociado, catalogos))
                .movimientos(extraerRecibosUnicos(cuentasPuesto, cuentasSocioAsociado).stream()
                        .map(recibo -> mapper.toReciboResponse(recibo, List.of(), catalogos))
                        .toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReferenciaResponse referencias(String tipo, Long id) {
        long cantidad = switch (tipo.toLowerCase()) {
            case "socio" -> cuentaPorCobrarRepository.countBySocioId(id);
            case "puesto" -> cuentaPorCobrarRepository.countByPuestoId(id);
            case "servicio" -> cuentaPorCobrarRepository.countByServicioId(id);
            case "banco" -> reciboRepository.countByBancoId(id);
            default -> throw new BusinessRuleException("Tipo de referencia no soportado: " + tipo);
        };
        return ReferenciaResponse.builder().tipo(tipo.toLowerCase()).id(id).cantidad(cantidad).build();
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

    private CuentaPorCobrar crearCuentaPorConsumo(ServicioCobrableResponse servicio, String periodo,
            LecturaPuestoRequest lectura) {
        Long puestoId = validarPuesto(lectura.getPuestoId());
        BigDecimal diferencia = lectura.getLecturaFinal().subtract(lectura.getLecturaInicial());
        BigDecimal importe = diferencia.compareTo(BigDecimal.ZERO) > 0
                ? diferencia.multiply(servicio.getCosto())
                : BigDecimal.ZERO;

        return CuentaPorCobrar.builder()
                .estado(EstadoCuenta.PENDIENTE)
                .monto(importe)
                .periodo(periodo)
                .puestoId(puestoId)
                .servicioId(servicio.getId())
                .lecturaInicial(lectura.getLecturaInicial())
                .lecturaFinal(lectura.getLecturaFinal())
                .build();
    }

    private List<SocioResponse> deduplicarPorNombreYApellido(List<SocioResponse> socios) {
        Map<String, SocioResponse> unicos = new LinkedHashMap<>();
        for (SocioResponse socio : socios) {
            String clave = (socio.getNombres().trim() + "|" + socio.getApellidos().trim()).toLowerCase();
            unicos.putIfAbsent(clave, socio);
        }
        return new ArrayList<>(unicos.values());
    }

    private void validarDestinatario(ServicioCobrableResponse servicio, TipoDestinatario esperado) {
        if (servicio.getDestinatario() != esperado) {
            throw new BusinessRuleException(
                    "El servicio '" + servicio.getNombre() + "' no esta configurado para destinatario " + esperado);
        }
    }

    /** Confirma con ms-catalogos que el puesto existe (si no, responde 404 con su mensaje). */
    private Long validarPuesto(Long puestoId) {
        return catalogosClient.obtenerPuesto(puestoId).getId();
    }

    private List<CuentaPorCobrarResponse> guardarYMapear(List<CuentaPorCobrar> cuentas, ServicioCobrableResponse servicio,
            String periodo) {
        List<CuentaPorCobrar> guardadas = cuentaPorCobrarRepository.saveAll(cuentas);
        auditoria.registrar("COBRANZA", "CUENTAS_GENERADAS", servicio.getNombre() + " " + periodo,
                guardadas.size() + " cuenta(s) generada(s)",
                guardadas.stream().map(CuentaPorCobrar::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add));
        return mapper.toCuentaResponses(guardadas, mapper.nuevoResolver());
    }

    private CuentaPorCobrar buscarOLanzar(Long id) {
        return cuentaPorCobrarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por cobrar no encontrada con id " + id));
    }
}
