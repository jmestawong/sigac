package com.cibertec.sigac.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.cibertec.sigac.entity.Egreso;
import com.cibertec.sigac.entity.EstadoEgreso;

@DataJpaTest
class EgresoRepositoryTest {

    @Autowired
    private EgresoRepository egresoRepository;

    private Egreso nuevoEgreso(String documento, LocalDate fecha, String importe) {
        return Egreso.builder()
                .numeroDocumento(documento)
                .proveedor("Ferreteria Central")
                .fecha(fecha)
                .importe(new BigDecimal(importe))
                .motivo("Compra de materiales")
                .estado(EstadoEgreso.REGISTRADO)
                .build();
    }

    @Test
    void insertar_debePersistirElEgresoConIdGenerado() {
        Egreso guardado = egresoRepository.save(nuevoEgreso("F-001", LocalDate.of(2026, 8, 5), "150.00"));

        Egreso persistido = egresoRepository.findById(guardado.getId()).orElseThrow();

        assertThat(persistido.getId()).isNotNull();
        assertThat(persistido.getImporte()).isEqualByComparingTo("150.00");
        assertThat(persistido.getEstado()).isEqualTo(EstadoEgreso.REGISTRADO);
    }

    @Test
    void listar_debeOrdenarPorFechaDescendenteYLuegoPorIdDescendente() {
        Egreso a = egresoRepository.save(nuevoEgreso("F-010", LocalDate.of(2026, 8, 1), "10.00"));
        Egreso b = egresoRepository.save(nuevoEgreso("F-011", LocalDate.of(2026, 8, 20), "20.00"));
        Egreso c = egresoRepository.save(nuevoEgreso("F-012", LocalDate.of(2026, 8, 20), "30.00"));

        List<Egreso> egresos = egresoRepository.findAllByOrderByFechaDescIdDesc();

        assertThat(egresos).extracting(Egreso::getId).containsExactly(c.getId(), b.getId(), a.getId());
    }

    @Test
    void actualizar_debeModificarElEstadoYElCorrelativo() {
        Egreso guardado = egresoRepository.save(nuevoEgreso("F-020", LocalDate.of(2026, 8, 5), "99.90"));

        guardado.setCorrelativo("E-000001");
        guardado.setEstado(EstadoEgreso.PROCESADO);
        egresoRepository.save(guardado);

        Egreso actualizado = egresoRepository.findById(guardado.getId()).orElseThrow();

        assertThat(actualizado.getCorrelativo()).isEqualTo("E-000001");
        assertThat(actualizado.getEstado()).isEqualTo(EstadoEgreso.PROCESADO);
    }

    @Test
    void eliminar_debeRemoverElEgresoDeLaBaseDeDatos() {
        Egreso guardado = egresoRepository.save(nuevoEgreso("F-030", LocalDate.of(2026, 8, 5), "45.00"));
        Long id = guardado.getId();

        egresoRepository.deleteById(id);

        assertThat(egresoRepository.findById(id)).isEmpty();
    }

    @Test
    void findByFechaBetween_debeFiltrarPorRangoInclusivoYOrdenar() {
        egresoRepository.save(nuevoEgreso("F-040", LocalDate.of(2026, 7, 31), "1.00"));
        Egreso dentroA = egresoRepository.save(nuevoEgreso("F-041", LocalDate.of(2026, 8, 1), "2.00"));
        Egreso dentroB = egresoRepository.save(nuevoEgreso("F-042", LocalDate.of(2026, 8, 31), "3.00"));
        egresoRepository.save(nuevoEgreso("F-043", LocalDate.of(2026, 9, 1), "4.00"));

        List<Egreso> deAgosto = egresoRepository.findByFechaBetweenOrderByFechaDescIdDesc(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertThat(deAgosto).extracting(Egreso::getId).containsExactly(dentroB.getId(), dentroA.getId());
    }
}
