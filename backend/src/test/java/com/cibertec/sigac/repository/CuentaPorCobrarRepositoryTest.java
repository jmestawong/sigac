package com.cibertec.sigac.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

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

/**
 * Pruebas del repositorio de CuentaPorCobrar: operaciones basicas
 * (insertar, listar, actualizar, eliminar) y las consultas derivadas que usa
 * la cobranza (por estado, por socio, por puesto, por socio propietario del
 * puesto y por recibo).
 */
@DataJpaTest
class CuentaPorCobrarRepositoryTest {

    @Autowired
    private CuentaPorCobrarRepository cuentaRepository;
    @Autowired
    private SocioRepository socioRepository;
    @Autowired
    private PuestoRepository puestoRepository;
    @Autowired
    private GiroRepository giroRepository;
    @Autowired
    private ServicioCobrableRepository servicioRepository;
    @Autowired
    private ReciboRepository reciboRepository;

    private Socio socio;
    private Puesto puesto;
    private ServicioCobrable servicio;

    @BeforeEach
    void prepararDatosBase() {
        socio = socioRepository.save(Socio.builder()
                .codigo("S-CXC").nombres("Rosa").apellidos("Quispe").accion("Ordinaria").etapa("1")
                .fechaNacimiento(LocalDate.of(1978, 3, 12)).build());
        Giro giro = giroRepository.save(Giro.builder().nombre("Abarrotes").build());
        puesto = puestoRepository.save(Puesto.builder()
                .numero("P-CXC").nombreInquilino("Carlos Ramos")
                .fechaInicioVigencia(LocalDate.of(2026, 1, 1)).fechaFinVigencia(LocalDate.of(2026, 12, 31))
                .giro(giro).socio(socio).build());
        servicio = servicioRepository.save(ServicioCobrable.builder()
                .nombre("Mantenimiento").recurrencia(Recurrencia.MENSUAL).costo(new BigDecimal("50.00"))
                .moneda(Moneda.PEN).destinatario(TipoDestinatario.PUESTO).esPorConsumo(false).build());
    }

    private CuentaPorCobrar cuentaDePuesto(String periodo, EstadoCuenta estado) {
        return CuentaPorCobrar.builder()
                .estado(estado).monto(new BigDecimal("50.00")).periodo(periodo)
                .puesto(puesto).servicio(servicio).build();
    }

    private CuentaPorCobrar cuentaDeSocio(String periodo) {
        return CuentaPorCobrar.builder()
                .estado(EstadoCuenta.PENDIENTE).monto(new BigDecimal("120.00")).periodo(periodo)
                .socio(socio).servicio(servicio).build();
    }

    @Test
    void insertar_debePersistirLaCuentaConSusRelaciones() {
        CuentaPorCobrar guardada = cuentaRepository.save(cuentaDePuesto("2026-10", EstadoCuenta.PENDIENTE));

        CuentaPorCobrar persistida = cuentaRepository.findById(guardada.getId()).orElseThrow();

        assertThat(persistida.getId()).isNotNull();
        assertThat(persistida.getPuesto().getId()).isEqualTo(puesto.getId());
        assertThat(persistida.getServicio().getId()).isEqualTo(servicio.getId());
        assertThat(persistida.getEstado()).isEqualTo(EstadoCuenta.PENDIENTE);
    }

    @Test
    void listar_debeRetornarLasCuentasDeLaMasRecienteALaMasAntigua() {
        CuentaPorCobrar primera = cuentaRepository.save(cuentaDePuesto("2026-09", EstadoCuenta.PENDIENTE));
        CuentaPorCobrar segunda = cuentaRepository.save(cuentaDePuesto("2026-10", EstadoCuenta.PENDIENTE));

        List<CuentaPorCobrar> cuentas = cuentaRepository.findAllByOrderByIdDesc();

        assertThat(cuentas).extracting(CuentaPorCobrar::getId).containsExactly(segunda.getId(), primera.getId());
    }

    @Test
    void actualizar_debeCambiarElEstadoYAsociarElRecibo() {
        CuentaPorCobrar guardada = cuentaRepository.save(cuentaDePuesto("2026-10", EstadoCuenta.PENDIENTE));
        Recibo recibo = reciboRepository.save(Recibo.builder()
                .tipo(TipoRecibo.PAGO_CUENTAS).fecha(LocalDateTime.of(2026, 10, 20, 10, 0))
                .monto(new BigDecimal("50.00")).build());

        guardada.setEstado(EstadoCuenta.ABONADA);
        guardada.setRecibo(recibo);
        cuentaRepository.save(guardada);

        CuentaPorCobrar actualizada = cuentaRepository.findById(guardada.getId()).orElseThrow();

        assertThat(actualizada.getEstado()).isEqualTo(EstadoCuenta.ABONADA);
        assertThat(actualizada.getRecibo().getId()).isEqualTo(recibo.getId());
    }

    @Test
    void eliminar_debeRemoverLaCuentaSinBorrarSuPuestoNiSuServicio() {
        CuentaPorCobrar guardada = cuentaRepository.save(cuentaDePuesto("2026-10", EstadoCuenta.PENDIENTE));
        Long id = guardada.getId();

        cuentaRepository.deleteById(id);

        assertThat(cuentaRepository.findById(id)).isEmpty();
        assertThat(puestoRepository.findById(puesto.getId())).isPresent();
        assertThat(servicioRepository.findById(servicio.getId())).isPresent();
    }

    @Test
    void findByEstado_debeFiltrarLasCuentasPorSuEstado() {
        cuentaRepository.save(cuentaDePuesto("2026-08", EstadoCuenta.PENDIENTE));
        CuentaPorCobrar abonada = cuentaRepository.save(cuentaDePuesto("2026-09", EstadoCuenta.ABONADA));
        cuentaRepository.save(cuentaDePuesto("2026-10", EstadoCuenta.EXONERADA));

        List<CuentaPorCobrar> abonadas = cuentaRepository.findByEstado(EstadoCuenta.ABONADA);

        assertThat(abonadas).extracting(CuentaPorCobrar::getId).containsExactly(abonada.getId());
    }

    @Test
    void consultasPorSocioYPorPuesto_debenSepararLasCuentasDirectasDeLasDelPuesto() {
        CuentaPorCobrar deSocio = cuentaRepository.save(cuentaDeSocio("2026"));
        CuentaPorCobrar dePuesto = cuentaRepository.save(cuentaDePuesto("2026-10", EstadoCuenta.PENDIENTE));

        assertThat(cuentaRepository.findBySocioIdOrderByIdDesc(socio.getId()))
                .extracting(CuentaPorCobrar::getId).containsExactly(deSocio.getId());
        assertThat(cuentaRepository.findByPuestoIdOrderByIdDesc(puesto.getId()))
                .extracting(CuentaPorCobrar::getId).containsExactly(dePuesto.getId());
        assertThat(cuentaRepository.findByPuesto_Socio_IdOrderByIdDesc(socio.getId()))
                .extracting(CuentaPorCobrar::getId).containsExactly(dePuesto.getId());
    }

    @Test
    void findByReciboId_debeRetornarSoloLasCuentasCubiertasPorEseRecibo() {
        Recibo recibo = reciboRepository.save(Recibo.builder()
                .tipo(TipoRecibo.PAGO_CUENTAS).fecha(LocalDateTime.of(2026, 10, 20, 10, 0))
                .monto(new BigDecimal("50.00")).build());
        CuentaPorCobrar cubierta = cuentaDePuesto("2026-10", EstadoCuenta.ABONADA);
        cubierta.setRecibo(recibo);
        cubierta = cuentaRepository.save(cubierta);
        cuentaRepository.save(cuentaDePuesto("2026-11", EstadoCuenta.PENDIENTE));

        List<CuentaPorCobrar> cuentas = cuentaRepository.findByReciboId(recibo.getId());

        assertThat(cuentas).extracting(CuentaPorCobrar::getId).containsExactly(cubierta.getId());
    }
}
