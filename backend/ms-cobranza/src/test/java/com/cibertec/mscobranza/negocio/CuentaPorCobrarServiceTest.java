package com.cibertec.mscobranza.negocio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cibertec.mscobranza.client.CatalogosClient;
import com.cibertec.mscobranza.dto.CuentaPorCobrarResponse;
import com.cibertec.mscobranza.dto.GenerarPuestosConsumoRequest;
import com.cibertec.mscobranza.dto.GenerarPuestosMontoFijoRequest;
import com.cibertec.mscobranza.dto.GenerarSociosRequest;
import com.cibertec.mscobranza.dto.GiroResponse;
import com.cibertec.mscobranza.dto.LecturaPuestoRequest;
import com.cibertec.mscobranza.dto.Moneda;
import com.cibertec.mscobranza.dto.PuestoResponse;
import com.cibertec.mscobranza.dto.Recurrencia;
import com.cibertec.mscobranza.dto.ReferenciaResponse;
import com.cibertec.mscobranza.dto.ResumenPuestoResponse;
import com.cibertec.mscobranza.dto.ResumenSocioResponse;
import com.cibertec.mscobranza.dto.ServicioCobrableResponse;
import com.cibertec.mscobranza.dto.SocioResponse;
import com.cibertec.mscobranza.dto.TipoDestinatario;
import com.cibertec.mscobranza.entidades.CuentaPorCobrar;
import com.cibertec.mscobranza.entidades.EstadoCuenta;
import com.cibertec.mscobranza.repositorio.CuentaPorCobrarRepository;
import com.cibertec.mscobranza.repositorio.ReciboRepository;
import com.cibertec.sigac.common.auditoria.AuditoriaPublisher;
import com.cibertec.sigac.common.exception.BusinessRuleException;
import com.cibertec.sigac.common.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class CuentaPorCobrarServiceTest {

    @Mock
    private CuentaPorCobrarRepository cuentaPorCobrarRepository;

    @Mock
    private ReciboRepository reciboRepository;

    @Mock
    private CatalogosClient catalogosClient;

    @Mock
    private AuditoriaPublisher auditoria;

    private CuentaPorCobrarServiceImpl cuentaPorCobrarService;

    private PuestoResponse puesto1;
    private PuestoResponse puesto2;
    private ServicioCobrableResponse servicioMontoFijoPuesto;
    private ServicioCobrableResponse servicioConsumoPuesto;
    private ServicioCobrableResponse servicioSocio;

    @BeforeEach
    void setUp() {
        cuentaPorCobrarService = new CuentaPorCobrarServiceImpl(cuentaPorCobrarRepository, reciboRepository,
                catalogosClient, new CobranzaMapper(catalogosClient), auditoria);

        GiroResponse giro = GiroResponse.builder().id(1L).nombre("Abarrotes").build();

        puesto1 = PuestoResponse.builder()
                .id(1L).numero("P-01").nombreInquilino("Juan Perez")
                .fechaInicioVigencia(LocalDate.of(2026, 1, 1)).fechaFinVigencia(LocalDate.of(2026, 12, 31))
                .giro(giro).build();

        puesto2 = PuestoResponse.builder()
                .id(2L).numero("P-02").nombreInquilino("Maria Lopez")
                .fechaInicioVigencia(LocalDate.of(2026, 1, 1)).fechaFinVigencia(LocalDate.of(2026, 12, 31))
                .giro(giro).build();

        servicioMontoFijoPuesto = ServicioCobrableResponse.builder()
                .id(1L).nombre("Mantenimiento").recurrencia(Recurrencia.MENSUAL)
                .costo(new BigDecimal("50.00")).moneda(Moneda.PEN)
                .destinatario(TipoDestinatario.PUESTO).esPorConsumo(false).build();

        servicioConsumoPuesto = ServicioCobrableResponse.builder()
                .id(2L).nombre("Agua").recurrencia(Recurrencia.MENSUAL)
                .costo(new BigDecimal("3.50")).moneda(Moneda.PEN)
                .destinatario(TipoDestinatario.PUESTO).esPorConsumo(true).build();

        servicioSocio = ServicioCobrableResponse.builder()
                .id(3L).nombre("Cuota asociativa").recurrencia(Recurrencia.ANUAL)
                .costo(new BigDecimal("100.00")).moneda(Moneda.PEN)
                .destinatario(TipoDestinatario.SOCIO).esPorConsumo(false).build();

        lenient().when(catalogosClient.obtenerServicio(1L)).thenReturn(servicioMontoFijoPuesto);
        lenient().when(catalogosClient.obtenerServicio(2L)).thenReturn(servicioConsumoPuesto);
        lenient().when(catalogosClient.obtenerServicio(3L)).thenReturn(servicioSocio);
        lenient().when(catalogosClient.obtenerPuesto(1L)).thenReturn(puesto1);
        lenient().when(catalogosClient.obtenerPuesto(2L)).thenReturn(puesto2);
    }

    private SocioResponse socio(long id, String codigo, String nombres, String apellidos, String etapa) {
        return SocioResponse.builder()
                .id(id).codigo(codigo).nombres(nombres).apellidos(apellidos)
                .accion("Ordinaria").etapa(etapa).fechaNacimiento(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    void generarParaPuestosMontoFijo_debeCrearUnaCuentaPorCadaPuesto() {
        GenerarPuestosMontoFijoRequest request = new GenerarPuestosMontoFijoRequest(
                1L, "2026-01", new BigDecimal("80.00"), List.of(1L, 2L));
        when(cuentaPorCobrarRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CuentaPorCobrarResponse> resultado = cuentaPorCobrarService.generarParaPuestosMontoFijo(request);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allSatisfy(c -> {
            assertThat(c.getMonto()).isEqualByComparingTo("80.00");
            assertThat(c.getEstado()).isEqualTo(EstadoCuenta.PENDIENTE);
            assertThat(c.getPeriodo()).isEqualTo("2026-01");
            assertThat(c.getServicio().getNombre()).isEqualTo("Mantenimiento");
        });
        assertThat(resultado).extracting(c -> c.getPuesto().getNumero()).containsExactly("P-01", "P-02");
        verify(auditoria).registrar(eq("COBRANZA"), eq("CUENTAS_GENERADAS"), anyString(), anyString(), any());
    }

    @Test
    void generarParaPuestosMontoFijo_cuandoServicioEsPorConsumo_debeLanzarExcepcion() {
        GenerarPuestosMontoFijoRequest request = new GenerarPuestosMontoFijoRequest(
                2L, "2026-01", new BigDecimal("80.00"), List.of(1L));

        assertThatThrownBy(() -> cuentaPorCobrarService.generarParaPuestosMontoFijo(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(cuentaPorCobrarRepository, never()).saveAll(anyList());
    }

    @Test
    void generarParaPuestosMontoFijo_cuandoServicioEsParaSocios_debeLanzarExcepcion() {
        GenerarPuestosMontoFijoRequest request = new GenerarPuestosMontoFijoRequest(
                3L, "2026-01", new BigDecimal("80.00"), List.of(1L));

        assertThatThrownBy(() -> cuentaPorCobrarService.generarParaPuestosMontoFijo(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void generarParaPuestosMontoFijo_cuandoElPuestoNoExisteEnCatalogos_propagaElNoEncontrado() {
        GenerarPuestosMontoFijoRequest request = new GenerarPuestosMontoFijoRequest(
                1L, "2026-01", new BigDecimal("80.00"), List.of(99L));
        when(catalogosClient.obtenerPuesto(99L))
                .thenThrow(new ResourceNotFoundException("Puesto no encontrado con id 99"));

        assertThatThrownBy(() -> cuentaPorCobrarService.generarParaPuestosMontoFijo(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(cuentaPorCobrarRepository, never()).saveAll(anyList());
    }

    @Test
    void generarParaPuestosConsumo_conDiferenciaPositiva_calculaImporteMultiplicandoPorCostoUnitario() {
        GenerarPuestosConsumoRequest request = new GenerarPuestosConsumoRequest(
                2L, "2026-01",
                List.of(new LecturaPuestoRequest(1L, new BigDecimal("100"), new BigDecimal("140"))));
        when(cuentaPorCobrarRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CuentaPorCobrarResponse> resultado = cuentaPorCobrarService.generarParaPuestosConsumo(request);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getMonto()).isEqualByComparingTo("140.00");
        assertThat(resultado.get(0).getLecturaInicial()).isEqualByComparingTo("100");
        assertThat(resultado.get(0).getLecturaFinal()).isEqualByComparingTo("140");
    }

    @Test
    void generarParaPuestosConsumo_conDiferenciaNegativaOCero_elImporteEsCero() {
        GenerarPuestosConsumoRequest request = new GenerarPuestosConsumoRequest(
                2L, "2026-01",
                List.of(
                        new LecturaPuestoRequest(1L, new BigDecimal("100"), new BigDecimal("90")),
                        new LecturaPuestoRequest(2L, new BigDecimal("50"), new BigDecimal("50"))));
        when(cuentaPorCobrarRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CuentaPorCobrarResponse> resultado = cuentaPorCobrarService.generarParaPuestosConsumo(request);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allSatisfy(c -> assertThat(c.getMonto()).isEqualByComparingTo("0"));
    }

    @Test
    void generarParaPuestosConsumo_cuandoServicioNoEsPorConsumo_debeLanzarExcepcion() {
        GenerarPuestosConsumoRequest request = new GenerarPuestosConsumoRequest(
                1L, "2026-01",
                List.of(new LecturaPuestoRequest(1L, new BigDecimal("100"), new BigDecimal("140"))));

        assertThatThrownBy(() -> cuentaPorCobrarService.generarParaPuestosConsumo(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void generarParaSocios_filtraPorEtapaIndicada() {
        GenerarSociosRequest request = new GenerarSociosRequest(3L, "2026", new BigDecimal("100"), List.of("1"), false);
        SocioResponse ana = socio(1, "S-001", "Ana", "Torres", "1");
        SocioResponse luis = socio(2, "S-002", "Luis", "Gomez", "2");
        when(catalogosClient.listarSocios()).thenReturn(List.of(ana, luis));
        when(catalogosClient.obtenerSocio(1L)).thenReturn(ana);
        when(cuentaPorCobrarRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CuentaPorCobrarResponse> resultado = cuentaPorCobrarService.generarParaSocios(request);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getSocio().getCodigo()).isEqualTo("S-001");
    }

    @Test
    void generarParaSocios_conSociosUnicos_evitaDuplicadosPorNombreYApellido() {
        GenerarSociosRequest request = new GenerarSociosRequest(3L, "2026", new BigDecimal("100"), null, true);
        SocioResponse s1 = socio(1, "S-001", "Ana", "Torres", "1");
        SocioResponse s2 = socio(2, "S-002", "ana", "torres", "2");
        SocioResponse s3 = socio(3, "S-003", "Luis", "Gomez", "1");
        when(catalogosClient.listarSocios()).thenReturn(List.of(s1, s2, s3));
        when(catalogosClient.obtenerSocio(1L)).thenReturn(s1);
        when(catalogosClient.obtenerSocio(3L)).thenReturn(s3);
        when(cuentaPorCobrarRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CuentaPorCobrarResponse> resultado = cuentaPorCobrarService.generarParaSocios(request);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(c -> c.getSocio().getCodigo()).containsExactlyInAnyOrder("S-001", "S-003");
    }

    @Test
    void generarParaSocios_cuandoServicioEsParaPuestos_debeLanzarExcepcion() {
        GenerarSociosRequest request = new GenerarSociosRequest(1L, "2026", new BigDecimal("100"), null, false);

        assertThatThrownBy(() -> cuentaPorCobrarService.generarParaSocios(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void generar_cuandoServicioNoExiste_propagaElNoEncontradoDeCatalogos() {
        when(catalogosClient.obtenerServicio(99L))
                .thenThrow(new ResourceNotFoundException("Servicio cobrable no encontrado con id 99"));

        GenerarSociosRequest request = new GenerarSociosRequest(99L, "2026", new BigDecimal("100"), null, false);

        assertThatThrownBy(() -> cuentaPorCobrarService.generarParaSocios(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarTodas_consultaCadaSocioUnaSolaVezAunqueTengaVariasCuentas() {
        SocioResponse ana = socio(1, "S-001", "Ana", "Torres", "1");
        when(catalogosClient.obtenerSocio(1L)).thenReturn(ana);
        when(cuentaPorCobrarRepository.findAllByOrderByIdDesc()).thenReturn(List.of(
                cuentaDeSocio(10L, 1L), cuentaDeSocio(11L, 1L), cuentaDeSocio(12L, 1L)));

        List<CuentaPorCobrarResponse> resultado = cuentaPorCobrarService.listarTodas();

        assertThat(resultado).hasSize(3);
        verify(catalogosClient, org.mockito.Mockito.times(1)).obtenerSocio(1L);
    }

    @Test
    void listarTodas_siElSocioYaNoExisteEnCatalogos_laCuentaSeMuestraSinSocio() {
        when(catalogosClient.obtenerSocio(7L)).thenThrow(new ResourceNotFoundException("no existe"));
        when(cuentaPorCobrarRepository.findAllByOrderByIdDesc()).thenReturn(List.of(cuentaDeSocio(10L, 7L)));

        List<CuentaPorCobrarResponse> resultado = cuentaPorCobrarService.listarTodas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getSocio()).isNull();
    }

    @Test
    void resumenPorSocio_incluyeLasCuentasDeSusPuestos() {
        SocioResponse ana = socio(1, "S-001", "Ana", "Torres", "1");
        PuestoResponse puestoDeAna = PuestoResponse.builder().id(1L).numero("P-01").socio(ana)
                .giro(GiroResponse.builder().id(1L).nombre("Abarrotes").build()).build();
        PuestoResponse puestoAjeno = PuestoResponse.builder().id(2L).numero("P-02")
                .giro(GiroResponse.builder().id(1L).nombre("Abarrotes").build()).build();
        when(catalogosClient.obtenerSocio(1L)).thenReturn(ana);
        when(catalogosClient.listarPuestos()).thenReturn(List.of(puestoDeAna, puestoAjeno));
        when(cuentaPorCobrarRepository.findBySocioIdOrderByIdDesc(1L)).thenReturn(List.of(cuentaDeSocio(10L, 1L)));
        when(cuentaPorCobrarRepository.findByPuestoIdInOrderByIdDesc(List.of(1L)))
                .thenReturn(List.of(cuentaDePuesto(20L, 1L)));

        ResumenSocioResponse resumen = cuentaPorCobrarService.resumenPorSocio(1L);

        assertThat(resumen.getSocio().getCodigo()).isEqualTo("S-001");
        assertThat(resumen.getCuentasSocio()).hasSize(1);
        assertThat(resumen.getCuentasPuestos()).hasSize(1);
    }

    @Test
    void resumenPorPuesto_incluyeLasCuentasDelSocioAsociado() {
        SocioResponse ana = socio(1, "S-001", "Ana", "Torres", "1");
        PuestoResponse puesto = PuestoResponse.builder().id(1L).numero("P-01").socio(ana)
                .giro(GiroResponse.builder().id(1L).nombre("Abarrotes").build()).build();
        when(catalogosClient.obtenerPuesto(1L)).thenReturn(puesto);
        when(catalogosClient.obtenerSocio(1L)).thenReturn(ana);
        when(cuentaPorCobrarRepository.findByPuestoIdOrderByIdDesc(1L)).thenReturn(List.of(cuentaDePuesto(20L, 1L)));
        when(cuentaPorCobrarRepository.findBySocioIdOrderByIdDesc(1L)).thenReturn(List.of(cuentaDeSocio(10L, 1L)));

        ResumenPuestoResponse resumen = cuentaPorCobrarService.resumenPorPuesto(1L);

        assertThat(resumen.getPuesto().getNumero()).isEqualTo("P-01");
        assertThat(resumen.getCuentasPuesto()).hasSize(1);
        assertThat(resumen.getCuentasSocioAsociado()).hasSize(1);
    }

    @Test
    void referencias_cuentaLasCuentasQueUsanElSocio() {
        when(cuentaPorCobrarRepository.countBySocioId(5L)).thenReturn(3L);

        ReferenciaResponse referencia = cuentaPorCobrarService.referencias("socio", 5L);

        assertThat(referencia.getCantidad()).isEqualTo(3L);
        assertThat(referencia.getTipo()).isEqualTo("socio");
    }

    @Test
    void referencias_cuentaLosRecibosQueUsanElBanco() {
        when(reciboRepository.countByBancoId(2L)).thenReturn(4L);

        assertThat(cuentaPorCobrarService.referencias("banco", 2L).getCantidad()).isEqualTo(4L);
    }

    @Test
    void referencias_conTipoDesconocido_debeLanzarExcepcion() {
        assertThatThrownBy(() -> cuentaPorCobrarService.referencias("marciano", 1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void eliminar_debeInvocarAlRepositorioYAuditar() {
        CuentaPorCobrar cuenta = cuentaDePuesto(1L, 1L);
        when(cuentaPorCobrarRepository.findById(1L)).thenReturn(Optional.of(cuenta));

        cuentaPorCobrarService.eliminar(1L);

        verify(cuentaPorCobrarRepository).delete(cuenta);
        verify(auditoria).registrar(eq("COBRANZA"), eq("CUENTA_ELIMINADA"), anyString(), anyString(), any());
    }

    @Test
    void eliminar_cuandoNoExiste_debeLanzarExcepcion() {
        when(cuentaPorCobrarRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuentaPorCobrarService.eliminar(1L)).isInstanceOf(ResourceNotFoundException.class);

        verify(cuentaPorCobrarRepository, never()).delete(any(CuentaPorCobrar.class));
    }

    private CuentaPorCobrar cuentaDeSocio(long id, long socioId) {
        return CuentaPorCobrar.builder().id(id).estado(EstadoCuenta.PENDIENTE).monto(BigDecimal.TEN)
                .periodo("2026").socioId(socioId).servicioId(3L).build();
    }

    private CuentaPorCobrar cuentaDePuesto(long id, long puestoId) {
        return CuentaPorCobrar.builder().id(id).estado(EstadoCuenta.PENDIENTE).monto(BigDecimal.TEN)
                .periodo("2026-01").puestoId(puestoId).servicioId(1L).build();
    }
}
