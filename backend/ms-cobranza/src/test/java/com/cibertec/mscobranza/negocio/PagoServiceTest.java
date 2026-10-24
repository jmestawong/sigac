package com.cibertec.mscobranza.negocio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import com.cibertec.mscobranza.dto.BancoResponse;
import com.cibertec.mscobranza.dto.CanjeBancarioRequest;
import com.cibertec.mscobranza.dto.GiroResponse;
import com.cibertec.mscobranza.dto.IngresoExternoRequest;
import com.cibertec.mscobranza.dto.Moneda;
import com.cibertec.mscobranza.dto.ProcesarPagoRequest;
import com.cibertec.mscobranza.dto.ProcesarPagoResponse;
import com.cibertec.mscobranza.dto.PuestoResponse;
import com.cibertec.mscobranza.dto.Recurrencia;
import com.cibertec.mscobranza.dto.ReciboResponse;
import com.cibertec.mscobranza.dto.ServicioCobrableResponse;
import com.cibertec.mscobranza.dto.SocioResponse;
import com.cibertec.mscobranza.dto.TipoDestinatario;
import com.cibertec.mscobranza.entidades.CuentaPorCobrar;
import com.cibertec.mscobranza.entidades.EstadoCuenta;
import com.cibertec.mscobranza.entidades.Recibo;
import com.cibertec.mscobranza.entidades.TipoRecibo;
import com.cibertec.mscobranza.repositorio.CuentaPorCobrarRepository;
import com.cibertec.mscobranza.repositorio.ReciboRepository;
import com.cibertec.sigac.common.auditoria.AuditoriaPublisher;
import com.cibertec.sigac.common.exception.BusinessRuleException;
import com.cibertec.sigac.common.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private CuentaPorCobrarRepository cuentaPorCobrarRepository;

    @Mock
    private ReciboRepository reciboRepository;

    @Mock
    private CatalogosClient catalogosClient;

    @Mock
    private AuditoriaPublisher auditoria;

    private PagoServiceImpl pagoService;

    private BancoResponse banco;

    @BeforeEach
    void setUp() {
        pagoService = new PagoServiceImpl(cuentaPorCobrarRepository, reciboRepository, catalogosClient,
                new CobranzaMapper(catalogosClient), auditoria);

        ServicioCobrableResponse servicio = ServicioCobrableResponse.builder()
                .id(1L).nombre("Cuota").recurrencia(Recurrencia.ANUAL)
                .costo(new BigDecimal("50")).moneda(Moneda.PEN)
                .destinatario(TipoDestinatario.SOCIO).esPorConsumo(false).build();

        SocioResponse socio = SocioResponse.builder()
                .id(1L).codigo("S-001").nombres("Ana").apellidos("Torres")
                .accion("Ordinaria").etapa("1").fechaNacimiento(LocalDate.of(1990, 1, 1)).build();

        PuestoResponse puesto = PuestoResponse.builder()
                .id(1L).numero("P-01").nombreInquilino("Juan Perez")
                .giro(GiroResponse.builder().id(1L).nombre("Abarrotes").build()).build();

        banco = BancoResponse.builder()
                .id(1L).nombre("BCP").numeroCuenta("193-1").cci("00219300123456").moneda(Moneda.PEN).build();

        lenient().when(catalogosClient.obtenerServicio(1L)).thenReturn(servicio);
        lenient().when(catalogosClient.obtenerSocio(1L)).thenReturn(socio);
        lenient().when(catalogosClient.obtenerPuesto(1L)).thenReturn(puesto);

        // Simula el AUTO_INCREMENT de MySQL: el primer save asigna id si aun no tiene.
        // lenient() porque varios tests de validacion nunca llegan a crear un recibo.
        lenient().when(reciboRepository.save(any(Recibo.class))).thenAnswer(inv -> {
            Recibo r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(1L);
            }
            return r;
        });
    }

    private CuentaPorCobrar cuentaPendiente(long id, BigDecimal monto, boolean deSocio) {
        CuentaPorCobrar.CuentaPorCobrarBuilder builder = CuentaPorCobrar.builder()
                .id(id).estado(EstadoCuenta.PENDIENTE).monto(monto).periodo("2026").servicioId(1L);
        return deSocio ? builder.socioId(1L).build() : builder.puestoId(1L).build();
    }

    // --- procesarPagoCuentas ---

    @Test
    void procesarPago_conCuentasAbonadas_creaUnReciboConElTotalYCorrelativo() {
        when(cuentaPorCobrarRepository.findById(1L)).thenReturn(Optional.of(cuentaPendiente(1L, new BigDecimal("50"), true)));
        when(cuentaPorCobrarRepository.findById(2L)).thenReturn(Optional.of(cuentaPendiente(2L, new BigDecimal("30"), true)));
        when(cuentaPorCobrarRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ProcesarPagoRequest request = new ProcesarPagoRequest(List.of(1L, 2L), List.of());

        ProcesarPagoResponse resultado = pagoService.procesarPagoCuentas(request);

        assertThat(resultado.getRecibo()).isNotNull();
        assertThat(resultado.getRecibo().getTipo()).isEqualTo(TipoRecibo.PAGO_CUENTAS);
        assertThat(resultado.getRecibo().getMonto()).isEqualByComparingTo("80");
        assertThat(resultado.getRecibo().getCorrelativo()).isEqualTo("R-000001");
        assertThat(resultado.getCuentasActualizadas()).hasSize(2);
        assertThat(resultado.getCuentasActualizadas()).allSatisfy(c -> {
            assertThat(c.getEstado()).isEqualTo(EstadoCuenta.ABONADA);
            assertThat(c.getSocio().getCodigo()).isEqualTo("S-001");
        });
        verify(auditoria).registrar(eq("COBRANZA"), eq("RECIBO_EMITIDO"), eq("R-000001"), anyString(), any());
    }

    @Test
    void procesarPago_conCuentasExoneradas_noCreaRecibo() {
        when(cuentaPorCobrarRepository.findById(1L)).thenReturn(Optional.of(cuentaPendiente(1L, new BigDecimal("50"), true)));
        when(cuentaPorCobrarRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ProcesarPagoRequest request = new ProcesarPagoRequest(List.of(), List.of(1L));

        ProcesarPagoResponse resultado = pagoService.procesarPagoCuentas(request);

        assertThat(resultado.getRecibo()).isNull();
        assertThat(resultado.getCuentasActualizadas()).hasSize(1);
        assertThat(resultado.getCuentasActualizadas().get(0).getEstado()).isEqualTo(EstadoCuenta.EXONERADA);
        verify(reciboRepository, never()).save(any(Recibo.class));
    }

    @Test
    void procesarPago_cuandoAmbasListasVacias_debeLanzarExcepcion() {
        ProcesarPagoRequest request = new ProcesarPagoRequest(List.of(), List.of());

        assertThatThrownBy(() -> pagoService.procesarPagoCuentas(request)).isInstanceOf(BusinessRuleException.class);

        verify(cuentaPorCobrarRepository, never()).saveAll(anyList());
    }

    @Test
    void procesarPago_cuandoUnaCuentaEstaEnAmbasListas_debeLanzarExcepcion() {
        ProcesarPagoRequest request = new ProcesarPagoRequest(List.of(1L), List.of(1L));

        assertThatThrownBy(() -> pagoService.procesarPagoCuentas(request)).isInstanceOf(BusinessRuleException.class);

        verify(cuentaPorCobrarRepository, never()).findById(any());
    }

    @Test
    void procesarPago_cuandoUnaCuentaAbonadaYaFueProcesada_noDebeCrearReciboNiAuditar() {
        // Simula que otra operacion ya proceso la cuenta 2 (estado ya no es PENDIENTE)
        // justo antes de confirmar este pago, para probar que no queda estado a medias.
        CuentaPorCobrar exoneradaValida = cuentaPendiente(1L, new BigDecimal("50"), true);
        CuentaPorCobrar yaAbonada = cuentaPendiente(2L, new BigDecimal("30"), true);
        yaAbonada.setEstado(EstadoCuenta.ABONADA);

        when(cuentaPorCobrarRepository.findById(1L)).thenReturn(Optional.of(exoneradaValida));
        when(cuentaPorCobrarRepository.findById(2L)).thenReturn(Optional.of(yaAbonada));

        ProcesarPagoRequest request = new ProcesarPagoRequest(List.of(2L), List.of(1L));

        assertThatThrownBy(() -> pagoService.procesarPagoCuentas(request)).isInstanceOf(BusinessRuleException.class);

        // Ningun recibo debe crearse: la validacion de "abonadas" corre despues de
        // guardar "exoneradas" en la misma transaccion, por lo que @Transactional
        // revertira tambien ese guardado al propagar la excepcion.
        verify(reciboRepository, never()).save(any(Recibo.class));
        verify(auditoria, never()).registrar(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void procesarPago_cuandoCuentaNoExiste_debeLanzarExcepcion() {
        when(cuentaPorCobrarRepository.findById(99L)).thenReturn(Optional.empty());

        ProcesarPagoRequest request = new ProcesarPagoRequest(List.of(99L), List.of());

        assertThatThrownBy(() -> pagoService.procesarPagoCuentas(request)).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- canjeBancario ---

    @Test
    void canjeBancario_debeCrearReciboYMarcarLaCuentaComoAbonada() {
        CuentaPorCobrar cuenta = cuentaPendiente(1L, new BigDecimal("50"), true);
        when(cuentaPorCobrarRepository.findById(1L)).thenReturn(Optional.of(cuenta));
        when(catalogosClient.obtenerBanco(1L)).thenReturn(banco);

        CanjeBancarioRequest request = new CanjeBancarioRequest(1L, 1L, LocalDate.of(2026, 3, 15));

        ReciboResponse resultado = pagoService.canjeBancario(request);

        assertThat(resultado.getTipo()).isEqualTo(TipoRecibo.CANJE_BANCARIO);
        assertThat(resultado.getMonto()).isEqualByComparingTo("50");
        assertThat(resultado.getBanco().getNombre()).isEqualTo("BCP");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ABONADA);
        assertThat(cuenta.getRecibo()).isNotNull();
        verify(auditoria).registrar(eq("COBRANZA"), eq("CANJE_BANCARIO"), eq("R-000001"), anyString(), any());
    }

    @Test
    void canjeBancario_cuandoLaCuentaEsDePuesto_debeLanzarExcepcion() {
        when(cuentaPorCobrarRepository.findById(1L)).thenReturn(Optional.of(cuentaPendiente(1L, new BigDecimal("50"), false)));

        CanjeBancarioRequest request = new CanjeBancarioRequest(1L, 1L, LocalDate.of(2026, 3, 15));

        assertThatThrownBy(() -> pagoService.canjeBancario(request)).isInstanceOf(BusinessRuleException.class);

        verify(reciboRepository, never()).save(any(Recibo.class));
    }

    @Test
    void canjeBancario_cuandoElBancoNoExisteEnCatalogos_propagaElNoEncontrado() {
        when(cuentaPorCobrarRepository.findById(1L)).thenReturn(Optional.of(cuentaPendiente(1L, new BigDecimal("50"), true)));
        when(catalogosClient.obtenerBanco(99L)).thenThrow(new ResourceNotFoundException("Banco no encontrado con id 99"));

        CanjeBancarioRequest request = new CanjeBancarioRequest(1L, 99L, LocalDate.of(2026, 3, 15));

        assertThatThrownBy(() -> pagoService.canjeBancario(request)).isInstanceOf(ResourceNotFoundException.class);

        verify(reciboRepository, never()).save(any(Recibo.class));
    }

    // --- registrarIngresoExterno ---

    @Test
    void registrarIngresoExterno_debeCrearReciboSinCuentasAsociadas() {
        IngresoExternoRequest request = new IngresoExternoRequest("Municipalidad", "Donacion", "Aporte anual", new BigDecimal("500"));

        ReciboResponse resultado = pagoService.registrarIngresoExterno(request);

        assertThat(resultado.getTipo()).isEqualTo(TipoRecibo.INGRESO_EXTERNO);
        assertThat(resultado.getMonto()).isEqualByComparingTo("500");
        assertThat(resultado.getDepositante()).isEqualTo("Municipalidad");
        assertThat(resultado.getCuentas()).isEmpty();
        assertThat(resultado.getCorrelativo()).isEqualTo("R-000001");
        verify(auditoria).registrar(eq("COBRANZA"), eq("INGRESO_EXTERNO"), eq("R-000001"), anyString(), any());
    }

    // --- consultas ---

    @Test
    void obtenerRecibo_cuandoNoExiste_debeLanzarExcepcion() {
        when(reciboRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.obtenerReciboPorId(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarRecibos_filtraPorTipo() {
        Recibo pago = Recibo.builder().id(1L).correlativo("R-000001").tipo(TipoRecibo.PAGO_CUENTAS)
                .fecha(java.time.LocalDateTime.of(2026, 10, 20, 9, 0)).monto(BigDecimal.TEN).build();
        Recibo ingreso = Recibo.builder().id(2L).correlativo("R-000002").tipo(TipoRecibo.INGRESO_EXTERNO)
                .fecha(java.time.LocalDateTime.of(2026, 10, 20, 10, 0)).monto(BigDecimal.ONE).build();
        when(reciboRepository.findAllByOrderByIdDesc()).thenReturn(List.of(ingreso, pago));
        when(cuentaPorCobrarRepository.findByReciboId(1L)).thenReturn(List.of());

        List<ReciboResponse> resultado = pagoService.listarRecibos(null, TipoRecibo.PAGO_CUENTAS);

        assertThat(resultado).extracting(ReciboResponse::getCorrelativo).containsExactly("R-000001");
    }
}
