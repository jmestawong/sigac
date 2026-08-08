package com.cibertec.sigac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cibertec.sigac.dto.CanjeBancarioRequest;
import com.cibertec.sigac.dto.IngresoExternoRequest;
import com.cibertec.sigac.dto.ProcesarPagoRequest;
import com.cibertec.sigac.dto.ProcesarPagoResponse;
import com.cibertec.sigac.dto.ReciboResponse;
import com.cibertec.sigac.entity.Banco;
import com.cibertec.sigac.entity.CuentaPorCobrar;
import com.cibertec.sigac.entity.EstadoCuenta;
import com.cibertec.sigac.entity.Giro;
import com.cibertec.sigac.entity.Moneda;
import com.cibertec.sigac.entity.Puesto;
import com.cibertec.sigac.entity.Recibo;
import com.cibertec.sigac.entity.Recurrencia;
import com.cibertec.sigac.entity.ServicioCobrable;
import com.cibertec.sigac.entity.Socio;
import com.cibertec.sigac.entity.TipoDestinatario;
import com.cibertec.sigac.entity.TipoRecibo;
import com.cibertec.sigac.exception.BusinessRuleException;
import com.cibertec.sigac.exception.ResourceNotFoundException;
import com.cibertec.sigac.repository.BancoRepository;
import com.cibertec.sigac.repository.CuentaPorCobrarRepository;
import com.cibertec.sigac.repository.ReciboRepository;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private CuentaPorCobrarRepository cuentaPorCobrarRepository;

    @Mock
    private ReciboRepository reciboRepository;

    @Mock
    private BancoRepository bancoRepository;

    @InjectMocks
    private PagoServiceImpl pagoService;

    private ServicioCobrable servicio;
    private Socio socio;
    private Banco banco;

    @BeforeEach
    void setUp() {
        servicio = ServicioCobrable.builder()
                .id(1L).nombre("Cuota").recurrencia(Recurrencia.ANUAL)
                .costo(new BigDecimal("50")).moneda(Moneda.PEN)
                .destinatario(TipoDestinatario.SOCIO).esPorConsumo(false).build();

        socio = Socio.builder()
                .id(1L).codigo("S-001").nombres("Ana").apellidos("Torres")
                .accion("Ordinaria").etapa("1").fechaNacimiento(LocalDate.of(1990, 1, 1)).build();

        banco = Banco.builder()
                .id(1L).nombre("BCP").numeroCuenta("193-1").cci("00219300123456").moneda(Moneda.PEN).build();

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
                .id(id).estado(EstadoCuenta.PENDIENTE).monto(monto).periodo("2026").servicio(servicio);
        return deSocio ? builder.socio(socio).build() : builder.puesto(puestoDePrueba()).build();
    }

    private Puesto puestoDePrueba() {
        Giro giro = Giro.builder().id(1L).nombre("Abarrotes").build();
        return Puesto.builder().id(1L).numero("P-01").nombreInquilino("Juan Perez")
                .fechaInicioVigencia(LocalDate.of(2026, 1, 1)).fechaFinVigencia(LocalDate.of(2026, 12, 31))
                .giro(giro).build();
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
        assertThat(resultado.getCuentasActualizadas()).allSatisfy(c -> assertThat(c.getEstado()).isEqualTo(EstadoCuenta.ABONADA));
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
    void procesarPago_cuandoUnaCuentaAbonadaYaFueProcesada_noDebeCrearReciboNiGuardarNada() {
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
        when(bancoRepository.findById(1L)).thenReturn(Optional.of(banco));

        CanjeBancarioRequest request = new CanjeBancarioRequest(1L, 1L, LocalDate.of(2026, 3, 15));

        ReciboResponse resultado = pagoService.canjeBancario(request);

        assertThat(resultado.getTipo()).isEqualTo(TipoRecibo.CANJE_BANCARIO);
        assertThat(resultado.getMonto()).isEqualByComparingTo("50");
        assertThat(resultado.getBanco().getNombre()).isEqualTo("BCP");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ABONADA);
        assertThat(cuenta.getRecibo()).isNotNull();
    }

    @Test
    void canjeBancario_cuandoLaCuentaEsDePuesto_debeLanzarExcepcion() {
        when(cuentaPorCobrarRepository.findById(1L)).thenReturn(Optional.of(cuentaPendiente(1L, new BigDecimal("50"), false)));

        CanjeBancarioRequest request = new CanjeBancarioRequest(1L, 1L, LocalDate.of(2026, 3, 15));

        assertThatThrownBy(() -> pagoService.canjeBancario(request)).isInstanceOf(BusinessRuleException.class);

        verify(reciboRepository, never()).save(any(Recibo.class));
    }

    @Test
    void canjeBancario_cuandoElBancoNoExiste_debeLanzarExcepcion() {
        when(cuentaPorCobrarRepository.findById(1L)).thenReturn(Optional.of(cuentaPendiente(1L, new BigDecimal("50"), true)));
        when(bancoRepository.findById(99L)).thenReturn(Optional.empty());

        CanjeBancarioRequest request = new CanjeBancarioRequest(1L, 99L, LocalDate.of(2026, 3, 15));

        assertThatThrownBy(() -> pagoService.canjeBancario(request)).isInstanceOf(ResourceNotFoundException.class);
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
    }
}
