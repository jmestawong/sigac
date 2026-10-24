package com.cibertec.mscobranza.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.cibertec.mscobranza.entidades.CuentaPorCobrar;
import com.cibertec.mscobranza.entidades.EstadoCuenta;
import com.cibertec.mscobranza.entidades.Recibo;
import com.cibertec.mscobranza.entidades.TipoRecibo;

@DataJpaTest
class CuentaPorCobrarRepositoryTest {

    private static final Long SOCIO_ID = 11L;
    private static final Long PUESTO_ID = 21L;
    private static final Long SERVICIO_ID = 31L;

    @Autowired
    private CuentaPorCobrarRepository cuentaRepository;
    @Autowired
    private ReciboRepository reciboRepository;

    private CuentaPorCobrar cuentaDePuesto(String periodo, EstadoCuenta estado) {
        return CuentaPorCobrar.builder()
                .estado(estado).monto(new BigDecimal("50.00")).periodo(periodo)
                .puestoId(PUESTO_ID).servicioId(SERVICIO_ID).build();
    }

    private CuentaPorCobrar cuentaDeSocio(String periodo) {
        return CuentaPorCobrar.builder()
                .estado(EstadoCuenta.PENDIENTE).monto(new BigDecimal("120.00")).periodo(periodo)
                .socioId(SOCIO_ID).servicioId(SERVICIO_ID).build();
    }

    @Test
    void insertar_debePersistirLaCuentaConLosIdentificadoresDeCatalogos() {
        CuentaPorCobrar guardada = cuentaRepository.save(cuentaDePuesto("2026-10", EstadoCuenta.PENDIENTE));

        CuentaPorCobrar persistida = cuentaRepository.findById(guardada.getId()).orElseThrow();

        assertThat(persistida.getId()).isNotNull();
        assertThat(persistida.getPuestoId()).isEqualTo(PUESTO_ID);
        assertThat(persistida.getServicioId()).isEqualTo(SERVICIO_ID);
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
    void eliminar_debeRemoverLaCuenta() {
        CuentaPorCobrar guardada = cuentaRepository.save(cuentaDePuesto("2026-10", EstadoCuenta.PENDIENTE));
        Long id = guardada.getId();

        cuentaRepository.deleteById(id);

        assertThat(cuentaRepository.findById(id)).isEmpty();
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

        assertThat(cuentaRepository.findBySocioIdOrderByIdDesc(SOCIO_ID))
                .extracting(CuentaPorCobrar::getId).containsExactly(deSocio.getId());
        assertThat(cuentaRepository.findByPuestoIdOrderByIdDesc(PUESTO_ID))
                .extracting(CuentaPorCobrar::getId).containsExactly(dePuesto.getId());
        assertThat(cuentaRepository.findByPuestoIdInOrderByIdDesc(List.of(PUESTO_ID, 99L)))
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

    @Test
    void conteos_porSocioPuestoYServicio_sonLosQueConsultaCatalogosAntesDeEliminar() {
        cuentaRepository.save(cuentaDeSocio("2026"));
        cuentaRepository.save(cuentaDePuesto("2026-10", EstadoCuenta.PENDIENTE));
        cuentaRepository.save(cuentaDePuesto("2026-11", EstadoCuenta.PENDIENTE));

        assertThat(cuentaRepository.countBySocioId(SOCIO_ID)).isEqualTo(1);
        assertThat(cuentaRepository.countByPuestoId(PUESTO_ID)).isEqualTo(2);
        assertThat(cuentaRepository.countByServicioId(SERVICIO_ID)).isEqualTo(3);
        assertThat(cuentaRepository.countBySocioId(999L)).isZero();
    }
}
