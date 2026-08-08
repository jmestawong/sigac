package com.cibertec.sigac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cibertec.sigac.dto.CuentaPorCobrarResponse;
import com.cibertec.sigac.dto.GenerarPuestosConsumoRequest;
import com.cibertec.sigac.dto.GenerarPuestosMontoFijoRequest;
import com.cibertec.sigac.dto.GenerarSociosRequest;
import com.cibertec.sigac.dto.LecturaPuestoRequest;
import com.cibertec.sigac.entity.CuentaPorCobrar;
import com.cibertec.sigac.entity.EstadoCuenta;
import com.cibertec.sigac.entity.Giro;
import com.cibertec.sigac.entity.Moneda;
import com.cibertec.sigac.entity.Puesto;
import com.cibertec.sigac.entity.Recurrencia;
import com.cibertec.sigac.entity.ServicioCobrable;
import com.cibertec.sigac.entity.Socio;
import com.cibertec.sigac.entity.TipoDestinatario;
import com.cibertec.sigac.exception.BusinessRuleException;
import com.cibertec.sigac.exception.ResourceNotFoundException;
import com.cibertec.sigac.repository.CuentaPorCobrarRepository;
import com.cibertec.sigac.repository.PuestoRepository;
import com.cibertec.sigac.repository.ServicioCobrableRepository;
import com.cibertec.sigac.repository.SocioRepository;

@ExtendWith(MockitoExtension.class)
class CuentaPorCobrarServiceTest {

    @Mock
    private CuentaPorCobrarRepository cuentaPorCobrarRepository;

    @Mock
    private ServicioCobrableRepository servicioCobrableRepository;

    @Mock
    private PuestoRepository puestoRepository;

    @Mock
    private SocioRepository socioRepository;

    @InjectMocks
    private CuentaPorCobrarServiceImpl cuentaPorCobrarService;

    private Giro giro;
    private Puesto puesto1;
    private Puesto puesto2;
    private ServicioCobrable servicioMontoFijoPuesto;
    private ServicioCobrable servicioConsumoPuesto;
    private ServicioCobrable servicioSocio;

    @BeforeEach
    void setUp() {
        giro = Giro.builder().id(1L).nombre("Abarrotes").build();

        puesto1 = Puesto.builder()
                .id(1L).numero("P-01").nombreInquilino("Juan Perez")
                .fechaInicioVigencia(LocalDate.of(2026, 1, 1)).fechaFinVigencia(LocalDate.of(2026, 12, 31))
                .giro(giro).build();

        puesto2 = Puesto.builder()
                .id(2L).numero("P-02").nombreInquilino("Maria Lopez")
                .fechaInicioVigencia(LocalDate.of(2026, 1, 1)).fechaFinVigencia(LocalDate.of(2026, 12, 31))
                .giro(giro).build();

        servicioMontoFijoPuesto = ServicioCobrable.builder()
                .id(1L).nombre("Mantenimiento").recurrencia(Recurrencia.MENSUAL)
                .costo(new BigDecimal("50.00")).moneda(Moneda.PEN)
                .destinatario(TipoDestinatario.PUESTO).esPorConsumo(false).build();

        servicioConsumoPuesto = ServicioCobrable.builder()
                .id(2L).nombre("Agua").recurrencia(Recurrencia.MENSUAL)
                .costo(new BigDecimal("3.50")).moneda(Moneda.PEN)
                .destinatario(TipoDestinatario.PUESTO).esPorConsumo(true).build();

        servicioSocio = ServicioCobrable.builder()
                .id(3L).nombre("Cuota asociativa").recurrencia(Recurrencia.ANUAL)
                .costo(new BigDecimal("100.00")).moneda(Moneda.PEN)
                .destinatario(TipoDestinatario.SOCIO).esPorConsumo(false).build();
    }

    private Socio socio(long id, String codigo, String nombres, String apellidos, String etapa) {
        return Socio.builder()
                .id(id).codigo(codigo).nombres(nombres).apellidos(apellidos)
                .accion("Ordinaria").etapa(etapa).fechaNacimiento(LocalDate.of(1990, 1, 1))
                .build();
    }

    // --- Puestos, monto fijo ---

    @Test
    void generarParaPuestosMontoFijo_debeCrearUnaCuentaPorCadaPuesto() {
        GenerarPuestosMontoFijoRequest request = new GenerarPuestosMontoFijoRequest(
                1L, "2026-01", new BigDecimal("80.00"), List.of(1L, 2L));

        when(servicioCobrableRepository.findById(1L)).thenReturn(Optional.of(servicioMontoFijoPuesto));
        when(puestoRepository.findById(1L)).thenReturn(Optional.of(puesto1));
        when(puestoRepository.findById(2L)).thenReturn(Optional.of(puesto2));
        when(cuentaPorCobrarRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CuentaPorCobrarResponse> resultado = cuentaPorCobrarService.generarParaPuestosMontoFijo(request);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allSatisfy(c -> {
            assertThat(c.getMonto()).isEqualByComparingTo("80.00");
            assertThat(c.getEstado()).isEqualTo(EstadoCuenta.PENDIENTE);
            assertThat(c.getPeriodo()).isEqualTo("2026-01");
        });
    }

    @Test
    void generarParaPuestosMontoFijo_cuandoServicioEsPorConsumo_debeLanzarExcepcion() {
        GenerarPuestosMontoFijoRequest request = new GenerarPuestosMontoFijoRequest(
                2L, "2026-01", new BigDecimal("80.00"), List.of(1L));

        when(servicioCobrableRepository.findById(2L)).thenReturn(Optional.of(servicioConsumoPuesto));

        assertThatThrownBy(() -> cuentaPorCobrarService.generarParaPuestosMontoFijo(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(cuentaPorCobrarRepository, never()).saveAll(anyList());
    }

    @Test
    void generarParaPuestosMontoFijo_cuandoServicioEsParaSocios_debeLanzarExcepcion() {
        GenerarPuestosMontoFijoRequest request = new GenerarPuestosMontoFijoRequest(
                3L, "2026-01", new BigDecimal("80.00"), List.of(1L));

        when(servicioCobrableRepository.findById(3L)).thenReturn(Optional.of(servicioSocio));

        assertThatThrownBy(() -> cuentaPorCobrarService.generarParaPuestosMontoFijo(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    // --- Puestos, por consumo ---

    @Test
    void generarParaPuestosConsumo_conDiferenciaPositiva_calculaImporteMultiplicandoPorCostoUnitario() {
        GenerarPuestosConsumoRequest request = new GenerarPuestosConsumoRequest(
                2L, "2026-01",
                List.of(new LecturaPuestoRequest(1L, new BigDecimal("100"), new BigDecimal("140"))));

        when(servicioCobrableRepository.findById(2L)).thenReturn(Optional.of(servicioConsumoPuesto));
        when(puestoRepository.findById(1L)).thenReturn(Optional.of(puesto1));
        when(cuentaPorCobrarRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CuentaPorCobrarResponse> resultado = cuentaPorCobrarService.generarParaPuestosConsumo(request);

        // (140 - 100) * 3.50 = 140.00
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

        when(servicioCobrableRepository.findById(2L)).thenReturn(Optional.of(servicioConsumoPuesto));
        when(puestoRepository.findById(1L)).thenReturn(Optional.of(puesto1));
        when(puestoRepository.findById(2L)).thenReturn(Optional.of(puesto2));
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

        when(servicioCobrableRepository.findById(1L)).thenReturn(Optional.of(servicioMontoFijoPuesto));

        assertThatThrownBy(() -> cuentaPorCobrarService.generarParaPuestosConsumo(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    // --- Socios ---

    @Test
    void generarParaSocios_filtraPorEtapaIndicada() {
        GenerarSociosRequest request = new GenerarSociosRequest(3L, "2026", new BigDecimal("100"), List.of("1"), false);

        when(servicioCobrableRepository.findById(3L)).thenReturn(Optional.of(servicioSocio));
        when(socioRepository.findAll()).thenReturn(List.of(
                socio(1, "S-001", "Ana", "Torres", "1"),
                socio(2, "S-002", "Luis", "Gomez", "2")));
        when(cuentaPorCobrarRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CuentaPorCobrarResponse> resultado = cuentaPorCobrarService.generarParaSocios(request);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getSocio().getCodigo()).isEqualTo("S-001");
    }

    @Test
    void generarParaSocios_conSociosUnicos_evitaDuplicadosPorNombreYApellido() {
        GenerarSociosRequest request = new GenerarSociosRequest(3L, "2026", new BigDecimal("100"), null, true);

        when(servicioCobrableRepository.findById(3L)).thenReturn(Optional.of(servicioSocio));
        when(socioRepository.findAll()).thenReturn(List.of(
                socio(1, "S-001", "Ana", "Torres", "1"),
                socio(2, "S-002", "ana", "torres", "2"),
                socio(3, "S-003", "Luis", "Gomez", "1")));
        when(cuentaPorCobrarRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CuentaPorCobrarResponse> resultado = cuentaPorCobrarService.generarParaSocios(request);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(c -> c.getSocio().getCodigo()).containsExactlyInAnyOrder("S-001", "S-003");
    }

    @Test
    void generarParaSocios_cuandoServicioEsParaPuestos_debeLanzarExcepcion() {
        GenerarSociosRequest request = new GenerarSociosRequest(1L, "2026", new BigDecimal("100"), null, false);

        when(servicioCobrableRepository.findById(1L)).thenReturn(Optional.of(servicioMontoFijoPuesto));

        assertThatThrownBy(() -> cuentaPorCobrarService.generarParaSocios(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void generar_cuandoServicioNoExiste_debeLanzarExcepcion() {
        when(servicioCobrableRepository.findById(99L)).thenReturn(Optional.empty());

        GenerarSociosRequest request = new GenerarSociosRequest(99L, "2026", new BigDecimal("100"), null, false);

        assertThatThrownBy(() -> cuentaPorCobrarService.generarParaSocios(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminar_debeInvocarAlRepositorio() {
        CuentaPorCobrar cuenta = CuentaPorCobrar.builder().id(1L).estado(EstadoCuenta.PENDIENTE)
                .monto(BigDecimal.TEN).periodo("2026-01").servicio(servicioMontoFijoPuesto).build();
        when(cuentaPorCobrarRepository.findById(1L)).thenReturn(Optional.of(cuenta));

        cuentaPorCobrarService.eliminar(1L);

        verify(cuentaPorCobrarRepository).delete(cuenta);
    }
}
