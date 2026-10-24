package com.cibertec.mscobranza.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.cibertec.mscobranza.entidades.Recibo;
import com.cibertec.mscobranza.entidades.TipoRecibo;

/**
 * Pruebas del repositorio de Recibo: operaciones basicas (insertar, listar,
 * actualizar, eliminar) y el conteo por banco que usa ms-catalogos.
 */
@DataJpaTest
class ReciboRepositoryTest {

    @Autowired
    private ReciboRepository reciboRepository;

    private Recibo nuevoRecibo(TipoRecibo tipo, String monto) {
        return Recibo.builder()
                .tipo(tipo)
                .fecha(LocalDateTime.of(2026, 10, 20, 9, 30))
                .monto(new BigDecimal(monto))
                .build();
    }

    @Test
    void insertar_debePersistirElReciboConIdGenerado() {
        Recibo guardado = reciboRepository.save(nuevoRecibo(TipoRecibo.PAGO_CUENTAS, "170.00"));

        Recibo persistido = reciboRepository.findById(guardado.getId()).orElseThrow();

        assertThat(persistido.getId()).isNotNull();
        assertThat(persistido.getMonto()).isEqualByComparingTo("170.00");
        assertThat(persistido.getTipo()).isEqualTo(TipoRecibo.PAGO_CUENTAS);
    }

    @Test
    void insertar_conBanco_debeConservarElIdDelBancoYLaFechaDeDeposito() {
        Recibo canje = nuevoRecibo(TipoRecibo.CANJE_BANCARIO, "120.00");
        canje.setBancoId(7L);
        canje.setFechaDeposito(LocalDate.of(2026, 10, 15));

        Recibo persistido = reciboRepository.findById(reciboRepository.save(canje).getId()).orElseThrow();

        assertThat(persistido.getBancoId()).isEqualTo(7L);
        assertThat(persistido.getFechaDeposito()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void listar_debeRetornarLosRecibosDelMasRecienteAlMasAntiguo() {
        Recibo primero = reciboRepository.save(nuevoRecibo(TipoRecibo.PAGO_CUENTAS, "10.00"));
        Recibo segundo = reciboRepository.save(nuevoRecibo(TipoRecibo.INGRESO_EXTERNO, "20.00"));

        List<Recibo> recibos = reciboRepository.findAllByOrderByIdDesc();

        assertThat(recibos).extracting(Recibo::getId).containsExactly(segundo.getId(), primero.getId());
    }

    @Test
    void actualizar_debeAsignarElCorrelativoEnLaSegundaFase() {
        Recibo guardado = reciboRepository.save(nuevoRecibo(TipoRecibo.INGRESO_EXTERNO, "500.00"));

        guardado.setCorrelativo("R-%06d".formatted(guardado.getId()));
        reciboRepository.save(guardado);

        Recibo actualizado = reciboRepository.findById(guardado.getId()).orElseThrow();

        assertThat(actualizado.getCorrelativo()).startsWith("R-0000");
    }

    @Test
    void eliminar_debeRemoverElReciboDeLaBaseDeDatos() {
        Recibo guardado = reciboRepository.save(nuevoRecibo(TipoRecibo.PAGO_CUENTAS, "30.00"));
        Long id = guardado.getId();

        reciboRepository.deleteById(id);

        assertThat(reciboRepository.findById(id)).isEmpty();
    }

    @Test
    void countByBancoId_cuentaSoloLosRecibosDeEseBanco() {
        Recibo delBanco = nuevoRecibo(TipoRecibo.CANJE_BANCARIO, "50.00");
        delBanco.setBancoId(3L);
        Recibo otroBanco = nuevoRecibo(TipoRecibo.CANJE_BANCARIO, "60.00");
        otroBanco.setBancoId(4L);
        reciboRepository.save(delBanco);
        reciboRepository.save(otroBanco);
        reciboRepository.save(nuevoRecibo(TipoRecibo.PAGO_CUENTAS, "70.00"));

        assertThat(reciboRepository.countByBancoId(3L)).isEqualTo(1);
        assertThat(reciboRepository.countByBancoId(99L)).isZero();
    }
}
