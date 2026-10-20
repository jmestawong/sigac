package com.cibertec.sigac.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.cibertec.sigac.entity.Banco;
import com.cibertec.sigac.entity.Moneda;
import com.cibertec.sigac.entity.Recibo;
import com.cibertec.sigac.entity.TipoRecibo;

@DataJpaTest
class ReciboRepositoryTest {

    @Autowired
    private ReciboRepository reciboRepository;

    @Autowired
    private BancoRepository bancoRepository;

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
    void insertar_conBanco_debeConservarLaRelacionYLaFechaDeDeposito() {
        Banco banco = bancoRepository.save(Banco.builder()
                .nombre("BCP").numeroCuenta("193-1-0-01").cci("00219300000000100101").moneda(Moneda.PEN).build());
        Recibo canje = nuevoRecibo(TipoRecibo.CANJE_BANCARIO, "120.00");
        canje.setBanco(banco);
        canje.setFechaDeposito(LocalDate.of(2026, 10, 15));

        Recibo persistido = reciboRepository.findById(reciboRepository.save(canje).getId()).orElseThrow();

        assertThat(persistido.getBanco().getId()).isEqualTo(banco.getId());
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
}
